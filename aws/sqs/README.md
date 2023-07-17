Amazon SQS integration:
To integrate Amazon SQS (Simple Queue Service) with your Spring Boot application, you can use the AWS SDK for Java. First, add the following dependency to your pom.xml file:

<dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-java-sdk-sqs</artifactId>
    <version>1.11.999</version>
</dependency>

Next, create a bean for the AmazonSQS client in your Spring configuration file:

@Configuration
public class AwsConfig {

    @Value("${aws.access.key}")
    private String accessKey;
 
    @Value("${aws.secret.key}")
    private String secretKey;
 
    @Value("${aws.region}")
    private String region;
 
    @Bean
    public AmazonSQS amazonSQSClient() {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        return AmazonSQSClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(region)
                .build();
    }
}

In this example, the accessKey, secretKey, and region properties are defined in an external properties file.

To use the AmazonSQS client in your application, inject it into your service or controller class:

@Service
public class MyService {

    @Autowired
    private AmazonSQS amazonSQSClient;
 
    public void sendMessage(String queueUrl, String message) {
        SendMessageRequest request = new SendMessageRequest()
            .withQueueUrl(queueUrl)
            .withMessageBody(message);
        amazonSQSClient.sendMessage(request);
    }
}

In this example, the sendMessage() method is used to send a message to an Amazon SQS queue.