package cn.keking.web.controller;

import cn.keking.model.FileAttribute;
import cn.keking.service.FileHandlerService;
import cn.keking.service.FilePreview;
import cn.keking.service.FilePreviewFactory;
import cn.keking.service.cache.CacheService;
import cn.keking.service.impl.OtherFilePreviewImpl;
import cn.keking.utils.KkFileUtils;
import cn.keking.utils.WebUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.opensagres.xdocreport.core.io.IOUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static cn.keking.service.FilePreview.PICTURE_FILE_PREVIEW_PAGE;

/**
 * @author yudian-it
 */
@Controller
public class OnlinePreviewController {

    public static final String BASE64_DECODE_ERROR_MSG = "Base64解码失败，请检查你的 %s 是否采用 Base64 + urlEncode 双重编码了！";
    private final Logger logger = LoggerFactory.getLogger(OnlinePreviewController.class);

    private final FilePreviewFactory previewFactory;
    private final CacheService cacheService;
    private final FileHandlerService fileHandlerService;
    private final OtherFilePreviewImpl otherFilePreview;
    private static final RestTemplate restTemplate = new RestTemplate();
    private static  final HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    private static final ObjectMapper mapper = new ObjectMapper();

    public OnlinePreviewController(FilePreviewFactory filePreviewFactory, FileHandlerService fileHandlerService, CacheService cacheService, OtherFilePreviewImpl otherFilePreview) {
        this.previewFactory = filePreviewFactory;
        this.fileHandlerService = fileHandlerService;
        this.cacheService = cacheService;
        this.otherFilePreview = otherFilePreview;
    }

    /**
     * @param path  base64加密的本地绝对路径
     * @param model
     * @param req
     * @return
     */
    @GetMapping(value = "/localPreview")
    public String localPreview(@RequestParam String path, Model model, HttpServletRequest req) {
        String encPath = FilenameUtils.getBaseName(path);
        String localPath = new String(java.util.Base64.getUrlDecoder().decode(encPath), StandardCharsets.UTF_8);
        String[] pathArray = localPath.split(";");
        String fileName;
        String baseUri = MvcUriComponentsBuilder.fromController(OnlinePreviewController.class).toUriString();
        if (pathArray.length > 1) {
            // 多图片预览
            fileName = "多图预览.jpg";
            List<String> imageUrls = new ArrayList<>();
            for (String pathItem : pathArray) {
                String encPathItem = java.util.Base64.getUrlEncoder().encodeToString(pathItem.getBytes(StandardCharsets.UTF_8));
                String previewUrl = String.format("%spreview/%s", baseUri, encPathItem);
                imageUrls.add(previewUrl);
            }
            model.addAttribute("imgUrls", imageUrls);
        } else {
            File file = new File(localPath);
            // 检测路径是否合法
            if (!file.isAbsolute() || !file.exists()) {
                logger.info("无法处理的路径：{}", localPath);
                String errorMsg = String.format(BASE64_DECODE_ERROR_MSG, "url");
                return otherFilePreview.notSupportedFile(model, errorMsg);
            }
            fileName = file.getName();
        }

        String previewUrl = String.format("%spreview/%s", baseUri, path + "?fullfilename=" + fileName);
        logger.info("正在预览本地文件: {}", localPath);
        model.addAttribute("local", true);
        model.addAttribute("localPath", localPath);
        model.addAttribute("encodedLocalPath", encPath);
        return onlinePreview(previewUrl, model, req);
    }

    @GetMapping(value = "/preview/{path}")
    public ResponseEntity<?> preview(@PathVariable String path) {
        String encPath = FilenameUtils.getBaseName(path);
        String localPath = new String(java.util.Base64.getUrlDecoder().decode(encPath), StandardCharsets.UTF_8);
        File file = new File(localPath);
        // 检测路径是否合法
        if (!file.isAbsolute() || !file.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename="
                        + "\""
                        + new String(file.getName().getBytes(), StandardCharsets.ISO_8859_1)
                        + "\"")
                .body(new FileSystemResource(file));
    }

    @GetMapping( "/onlinePreview")
    public String onlinePreview(String url, Model model, HttpServletRequest req) {

        String fileUrl;
        try {
            fileUrl = WebUtils.decodeUrl(url);
        } catch (Exception ex) {
            String errorMsg = String.format(BASE64_DECODE_ERROR_MSG, "url");
            return otherFilePreview.notSupportedFile(model, errorMsg);
        }
        FileAttribute fileAttribute = fileHandlerService.getFileAttribute(fileUrl, req);  //这里不在进行URL 处理了
        model.addAttribute("file", fileAttribute);
        FilePreview filePreview = previewFactory.get(fileAttribute);
        logger.info("预览文件url：{}，previewType：{}", fileUrl, fileAttribute.getType());
        fileUrl =WebUtils.urlEncoderencode(fileUrl);
        if (ObjectUtils.isEmpty(fileUrl)) {
            return otherFilePreview.notSupportedFile(model, "非法路径,不允许访问");
        }
        return filePreview.filePreviewHandle(fileUrl, model, fileAttribute);  //统一在这里处理 url
    }

    @GetMapping( "/picturesPreview")
    public String picturesPreview(String urls, Model model, HttpServletRequest req) {
        String fileUrls;
        try {
            fileUrls = WebUtils.decodeUrl(urls);
            // 防止XSS攻击
            fileUrls = KkFileUtils.htmlEscape(fileUrls);
        } catch (Exception ex) {
            String errorMsg = String.format(BASE64_DECODE_ERROR_MSG, "urls");
            return otherFilePreview.notSupportedFile(model, errorMsg);
        }
        logger.info("预览文件url：{}，urls：{}", fileUrls, urls);
        // 抽取文件并返回文件列表
        String[] images = fileUrls.split("\\|");
        List<String> imgUrls = Arrays.asList(images);
        model.addAttribute("imgUrls", imgUrls);
        String currentUrl = req.getParameter("currentUrl");
        if (StringUtils.hasText(currentUrl)) {
            String decodedCurrentUrl = new String(Base64.decodeBase64(currentUrl));
                   decodedCurrentUrl = KkFileUtils.htmlEscape(decodedCurrentUrl);   // 防止XSS攻击
            model.addAttribute("currentUrl", decodedCurrentUrl);
        } else {
            model.addAttribute("currentUrl", imgUrls.get(0));
        }
        return PICTURE_FILE_PREVIEW_PAGE;
    }

    /**
     * 根据url获取文件内容
     * 当pdfjs读取存在跨域问题的文件时将通过此接口读取
     *
     * @param urlPath  url
     * @param response response
     */
    @GetMapping("/getCorsFile")
    public void getCorsFile(String urlPath, HttpServletResponse response,FileAttribute fileAttribute) throws IOException {
        URL url;
        try {
            urlPath = WebUtils.decodeUrl(urlPath);
            url = WebUtils.normalizedURL(urlPath);
        } catch (Exception ex) {
            logger.error(String.format(BASE64_DECODE_ERROR_MSG, urlPath),ex);
            return;
        }
        assert urlPath != null;
        if (!urlPath.toLowerCase().startsWith("http") && !urlPath.toLowerCase().startsWith("https") && !urlPath.toLowerCase().startsWith("ftp")) {
            logger.info("读取跨域文件异常，可能存在非法访问，urlPath：{}", urlPath);
            return;
        }
        InputStream inputStream = null;
        logger.info("读取跨域pdf文件url：{}", urlPath);
        if (!urlPath.toLowerCase().startsWith("ftp:")) {
            factory.setConnectionRequestTimeout(2000);
            factory.setConnectTimeout(10000);
            factory.setReadTimeout(72000);
            HttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(new DefaultRedirectStrategy()).build();
            factory.setHttpClient(httpClient);
            restTemplate.setRequestFactory(factory);
            RequestCallback requestCallback = request -> {
                request.getHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
                String proxyAuthorization = fileAttribute.getKkProxyAuthorization();
                if(StringUtils.hasText(proxyAuthorization)){
                    Map<String,String> proxyAuthorizationMap = mapper.readValue(proxyAuthorization, Map.class);
                    proxyAuthorizationMap.forEach((key, value) -> request.getHeaders().set(key, value));
                }
            };
            try {
                restTemplate.execute(url.toURI(), HttpMethod.GET, requestCallback, fileResponse -> {
                    IOUtils.copy(fileResponse.getBody(), response.getOutputStream());
                    return null;
                });
            }  catch (Exception e) {
                System.out.println(e);
            }
        }else{
            try {
                if(urlPath.contains(".svg")) {
                    response.setContentType("image/svg+xml");
                }
                inputStream = (url).openStream();
                IOUtils.copy(inputStream, response.getOutputStream());
            } catch (IOException e) {
                logger.error("读取跨域文件异常，url：{}", urlPath);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
    }

    /**
     * 通过api接口入队
     *
     * @param url 请编码后在入队
     */
    @GetMapping("/addTask")
    @ResponseBody
    public String addQueueTask(String url) {
        logger.info("添加转码队列url：{}", url);
        cacheService.addQueueTask(url);
        return "success";
    }
}
