import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class LambdaFunctionHandler implements RequestHandler<SNSEvent, Object> {
    private final String from = "no-reply@prod.pengchengxu.me";
    private final String subject = "Verification Email";

    private String region = "us-east-1";


    @Override
    public Object handleRequest(SNSEvent input, Context context) {

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        context.getLogger().log("Invocation started: " + timeStamp);

        AWSCredentials awsCredentials = new BasicAWSCredentials("AKIAVMPSPWKHQKFZLVPL",
                "FpCY9r1lswGZpy/8qQDE9PWznk6d3haDx0Bw2dCY");

        String record = input.getRecords().get(0).getSNS().getMessage();
        String[] list = record.split(";");
        context.getLogger().log("Email: " + list[0] + ", Token: " + list[1]);

        context.getLogger().log("before amazon");

        AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
                    .withRegion(region)
                    .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                    .build();

        context.getLogger().log("AmazonDynamo");

        DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        Table table = dynamoDB.getTable("verification");
        System.out.println(table.getItem("email", "patrick0929@outlook.com"));

        GetItemSpec spec = new GetItemSpec().withPrimaryKey("email", list[0]);

        try {
            System.out.println("Attempting to read the item...");
            Item outcome = table.getItem(spec);
            System.out.println("GetItem succeeded: " + outcome);
            if (outcome == null) {
                System.out.println("Outcome is null");
                return null;
            } else {
                if (outcome.getBoolean("isSend")) {
                    System.out.println("Email has been sent already");
                    return null;
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        String to = list[0];

        String htmlBody = "<p>Dear Customer: <br>Thank you for sign up an account on our website! " +
                "Here is your verification link: <a href='http://prod.pengchengxu.me/v1/verifyUserEmail?email=" + list[0] +
                "&token=" + list[1] + "'>http://prod.pengchengxu.me/v1/verifyUserEmail?email=" + list[0] + "&token=" + list[1] +
                "</a><br>Please Verify your Email Address in 5 minutes! <br> <br>Best!<p>";

        String textBody = "Dear Customer: \r\nThank you for sign up an account on our website! " +
                "Here is your verification link: http://prod.pengchengxu.me/v1/verifyUserEmail?email=" + list[0] + "&token=" + list[1] +
                "\r\nPlease Verify your Email Address in 5 minutes! \r\n \r\nBest!";

        UpdateItemSpec updateItemSpec = new UpdateItemSpec().withPrimaryKey("email", list[0])
                .withUpdateExpression("set isSend = :r")
                .withValueMap(new ValueMap().withBoolean(":r", true))
                .withReturnValues(ReturnValue.UPDATED_NEW);

        try {
            AmazonSimpleEmailService ses = AmazonSimpleEmailServiceClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                    .withRegion(region).build();
            SendEmailRequest request = new SendEmailRequest()
                    .withDestination(new Destination().withToAddresses(to))
                    .withMessage(new Message()
                            .withSubject(new Content().withCharset("UTF-8").withData(subject))
                            .withBody(new Body()
                                    .withHtml(new Content().withCharset("UTF-8").withData(htmlBody))
                                    .withText(new Content().withCharset("UTF-8").withData(textBody))))
                    .withSource(from);
            ses.sendEmail(request);
            System.out.println("Updating the item...");
            UpdateItemOutcome outcome = table.updateItem(updateItemSpec);
            System.out.println("UpdateItem succeeded:\n" + outcome.getItem().toJSONPretty());
            System.out.println("Email sent!");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
}
