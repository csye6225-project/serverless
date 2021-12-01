import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;

public class LambdaFunctionHandler implements RequestHandler<SNSEvent, Object> {
    private final String access_key = "AKIA5XV4MDZLBZFRGURF";
    private final String secret_key = "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";
    private final String from = "no-reply@prod.pengchengxu.me";
    private final String subject = "Verification Code";

    private final String region = "us-east-1";


    @Override
    public Object handleRequest(SNSEvent input, Context context) {

        String record = input.getRecords().get(0).getSNS().getMessage();
        String[] list = record.split(";");

        String to = list[0];

        String body = "Dear Customer: \r\nThank you for sign up an account on our website! \r\n " +
                "Here is your verification link: http://prod.pengchengxu.me/v1/verifyUserEmail?email=" + list[0] +
                "&token=" + list[1] + "\r\nPlease Verify your Email Address in 5 minutes! \r\n \r\nBest! \r\nXu's Company";



        try {
            AWSCredentials awsCredentials = new BasicAWSCredentials(access_key,
                    secret_key);
            AmazonSimpleEmailService ses = AmazonSimpleEmailServiceClientBuilder
                    .standard().withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                    .withRegion(region).build();
            SendEmailRequest request = new SendEmailRequest()
                    .withDestination(new Destination().withToAddresses(to))
                    .withMessage(new Message()
                            .withSubject(new Content().withCharset("UTF-8").withData(subject))
                            .withBody(new Body().withText(new Content().withCharset("UTF-8").withData(body))))
                    .withSource(from);
            ses.sendEmail(request);
            System.out.println("Email sent!");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
}
