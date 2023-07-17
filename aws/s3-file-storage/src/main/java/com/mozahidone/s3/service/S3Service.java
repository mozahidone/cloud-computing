package com.mozahidone.s3.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.amazonaws.services.s3.model.CannedAccessControlList.Private;

@Service
@Slf4j
@Getter
public class S3Service {
    private AmazonS3 amazonS3Client;
    private String bucketName;
    private String awsRegion;

    @Autowired
    public S3Service(@Value("${cloud.aws.credentials.accessKey}") String accessKey,
                     @Value("${cloud.aws.credentials.secretKey}") String secretKey,
                     @Value("${cloud.aws.region.static}") String region,
                     @Value("${cloud.aws.s3.excelBucketName}") String bucketName
    ) {
        this.amazonS3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                .withRegion(region)
                .build();
        this.bucketName = bucketName;
        this.awsRegion = region;
    }

    public File download(String bucket, String key) throws IOException {
        String fileName = key;
        if (key.lastIndexOf("/") != -1) {
            fileName = key.substring(key.lastIndexOf("/") + 1);
        }
        File file = Files.createTempFile("", fileName).toFile();
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key);
        ObjectMetadata objectMetadata = amazonS3Client.getObject(getObjectRequest, file);
        return file;
    }

    public String upload(String folder, String fileName, File file) {
        String s3ObjectKey = folder + "/" + System.currentTimeMillis() + "-" + fileName;
        PutObjectRequest request = new PutObjectRequest(bucketName, s3ObjectKey, file).withCannedAcl(Private);
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(getMimeType(file));
        request.setMetadata(objectMetadata);
        PutObjectResult putObjectResult = amazonS3Client.putObject(request);

        log.debug("s3ObjectKey: " + s3ObjectKey + ", ETag: " + putObjectResult.getETag());
        return s3ObjectKey;
    }

    public String getUrl(String key) {
        return "https://" + bucketName + ".s3." + awsRegion + ".amazonaws.com/" + key;
    }

    public String getKey(String s3URL) {
        String key = cleanUpURL(s3URL).replace(("https://s3-" + awsRegion + ".amazonaws.com/" + bucketName + "/"), "");
        key = key.replace("https://" + bucketName + ".s3." + awsRegion + ".amazonaws.com/", "");
        key = key.replace("https://" + bucketName + ".s3-" + awsRegion + ".amazonaws.com/", "");
        return key;
    }

    public static String cleanUpURL(String s3Url) {
        if (StringUtils.isEmpty(s3Url))
            return null;

        return s3Url.contains("?") ? s3Url.substring(0, s3Url.indexOf("?")) : s3Url;
    }

    public String generatePreSignedURL(String s3URL, int expireLinkInHours) {
        /*if (isEmpty(s3URL))
            return null;*/

        String key = getKey(s3URL);

        // Set the presigned URL to expire after one hour.
        int hour = expireLinkInHours;
        java.util.Date expiration = new java.util.Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 60 * hour;
        expiration.setTime(expTimeMillis);

        // Generate the presigned URL.
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucketName, key)
                        .withMethod(HttpMethod.GET)
                        .withExpiration(expiration);
        URL url = amazonS3Client.generatePresignedUrl(generatePresignedUrlRequest);

        return url.toString();
    }

    public String getMimeType(File file) {
        try {
            Path path = new File(file.getName()).toPath();
            String mimeType = Files.probeContentType(path);
            return mimeType;
        } catch (IOException e) {
            MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();
            String contentType = mimetypesFileTypeMap.getContentType(file);
            return contentType;
        }
    }

    /*public void upload(String path,
                       String fileName,
                       ObjectMetadata objectMetadata,
                       InputStream inputStream) {
        try {
            PutObjectResult putObjectResult= amazonS3Client.putObject(path, fileName, inputStream, objectMetadata);
            System.out.println(putObjectResult.getMetadata());
        } catch (AmazonServiceException e) {
            throw new IllegalStateException("Failed to upload the file", e.getCause());
        }
    }*/

    public void upload(PutObjectRequest putObjectRequest) {
        try {

            PutObjectResult putObjectResult=  amazonS3Client.putObject(putObjectRequest);
            System.out.println(putObjectResult.getMetadata());
        } catch (AmazonServiceException e) {
            throw new IllegalStateException("Failed to upload the file", e.getCause());
        }
    }

    public List<S3ObjectSummary> listObjects(String bucketName){
        ObjectListing objectListing = amazonS3Client.listObjects(bucketName);
        //return amazonS3Client.getObject(bucketName, key);
        return objectListing.getObjectSummaries();
    }

    public void deleteObject(String bucketName, String objectName){
        amazonS3Client.deleteObject(bucketName, objectName);
    }
}
