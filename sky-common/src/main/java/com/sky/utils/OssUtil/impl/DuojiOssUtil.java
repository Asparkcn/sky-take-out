package com.sky.utils.OssUtil.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.properties.DuojiOssProperties;
import com.sky.utils.OssUtil.OssUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "sky.oss", name = "type", havingValue = "Duoji")
public class DuojiOssUtil implements OssUtil {

    @Autowired
    private DuojiOssProperties duojiOssProperties;

    @Override
    public String upload(MultipartFile multipartFile) throws Exception {
        InputStream inputStream = multipartFile.getInputStream();

        String originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("文件名不能为空");
        }
        String extFilename = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = UUID.randomUUID() + extFilename;

        JSONObject body = new JSONObject();
        body.put("channel", "OSS_FULL");
        body.put("scopes", "*");
        JSONObject api = dogeAPIGet("/auth/tmp_token.json", body);
        JSONObject credentials = api.getJSONObject("credentials");
        String bucket = api.getString("s3Bucket");

        AwsSessionCredentials awsCreds = AwsSessionCredentials.create(credentials.getString("accessKeyId"), credentials.getString("secretAccessKey"), credentials.getString("sessionToken"));
        try (S3Client s3 = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(Region.of("automatic"))
                .endpointOverride(URI.create(duojiOssProperties.getEndpoint())) // 修改为多吉云控制台存储空间 SDK 参数中的 s3Endpoint
                .build()) {

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(newFilename)
                    .build();
            PutObjectResponse response = s3.putObject(putObjectRequest, software.amazon.awssdk.core.sync.RequestBody.fromInputStream(inputStream, multipartFile.getSize()));

            if (!response.sdkHttpResponse().isSuccessful()) {
                throw new RuntimeException("文件上传失败: " + response.sdkHttpResponse().statusText().orElse("未知错误"));
            }
        }

        return duojiOssProperties.getDomain() + '/' + newFilename;
    }

    private JSONObject dogeAPIGet(String apiPath, Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> hm : params.entrySet()) {
            try {
                sb.append(URLEncoder.encode(hm.getKey(), String.valueOf(StandardCharsets.UTF_8))).append('=').append(URLEncoder.encode(hm.getValue(), String.valueOf(StandardCharsets.UTF_8))).append("&");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        String bodyText = sb.toString().replace("&$", "");
        try {
            return dogeAPIGet(apiPath, bodyText, false);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    private JSONObject dogeAPIGet(String apiPath, JSONObject params) {
        String bodyText = params.toString();
        try {
            return dogeAPIGet(apiPath, bodyText, true);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    // 无参数 API
    private JSONObject dogeAPIGet(String apiPath) {
        try {
            return dogeAPIGet(apiPath, "", true);
        } catch (IOException e) {
            log.error("Error occurred while calling Doge API with path: {}", apiPath, e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private JSONObject dogeAPIGet(String apiPath, String paramsText, Boolean jsonMode) throws IOException {
        String signStr = apiPath + "\n" + paramsText;
        String sign;
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(duojiOssProperties.getAccessKeySecret().getBytes(), "HmacSHA1"));
            sign = new String(new Hex().encode(mac.doFinal(signStr.getBytes())), StandardCharsets.UTF_8); // 这里 Hex 来自 org.apache.commons.codec.binary.Hex
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        String authorization = "TOKEN " + duojiOssProperties.getAccessKeyId() + ':' + sign;

        URL u = new URL("https://api.dogecloud.com" + apiPath);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", jsonMode ? "application/json" : "application/x-www-form-urlencoded");
        conn.setRequestProperty("Authorization", authorization);
        conn.setRequestProperty("Content-Length", String.valueOf(paramsText.length()));
        OutputStream os = conn.getOutputStream();
        os.write(paramsText.getBytes());
        os.flush();
        os.close();
        StringBuilder retJSON = new StringBuilder();
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            String readLine;
            try (BufferedReader responseReader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                while ((readLine = responseReader.readLine()) != null) {
                    retJSON.append(readLine).append("\n");
                }
            }
            JSONObject ret = JSONObject.parseObject(retJSON.toString());
            if (Integer.parseInt(ret.get("code").toString()) != 200) {
                System.err.println("{\"error\":\"API 返回错误：" + ret.getString("msg") + "\"}");
            } else {
                JSONObject output = new JSONObject();
                JSONObject data = ret.getJSONObject("data");
                JSONObject bucket = data.getJSONArray("Buckets").getJSONObject(0);
                output.put("credentials", data.getJSONObject("Credentials"));
                output.put("s3Bucket", bucket.getString("s3Bucket"));
                output.put("s3Endpoint", bucket.getString("s3Endpoint"));
                output.put("keyPrefix", "*");
                return output;
            }
        } else {
            System.err.println("{\"error\":\"网络错误：" + conn.getResponseCode() + "\"}");
        }
        return null;
    }

}
