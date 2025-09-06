package cn.keking.web.controller;

import cn.keking.model.FileAttribute;
import cn.keking.service.OfficeToPdfService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jodconverter.core.office.OfficeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("convert")
public class ConvertController {
    private final Logger logger = LoggerFactory.getLogger(ConvertController.class);
    private final OfficeToPdfService officeToPdfService;

    private final OnlinePreviewController previewController;

    public ConvertController(OfficeToPdfService officeToPdfService, OnlinePreviewController previewController) {
        this.officeToPdfService = officeToPdfService;
        this.previewController = previewController;
    }

    @PostMapping("toPdf")
    public ResponseEntity<?> toPdf(@RequestPart("file") MultipartFile file) throws IOException, OfficeException {
        String dir = FileUtils.getTempDirectoryPath();
        String originalFilename = file.getOriginalFilename();
        String fileExt = FilenameUtils.getExtension(originalFilename);
        Path srcPath = Paths.get(dir, UUID.randomUUID() + "." + fileExt);
        file.transferTo(srcPath);
        File dstFile = new File(dir, UUID.randomUUID() + ".pdf");
        officeToPdfService.office2pdf(srcPath.toString(), dstFile.getAbsolutePath(), new FileAttribute());

        String dstFileName = FilenameUtils.removeExtension(originalFilename) + ".pdf";
        if (dstFile.exists()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + dstFileName)
                    .body(new FileSystemResource(dstFile));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("转换出错");
    }

    /**
     * 图片预览预处理
     *
     * @param paths 格式按照localPreview的path传参规则：[ encPath.后缀 ]
     * @return
     */
    @PostMapping("preHandlePics")
    public String preHandlePics(@RequestBody List<String> paths, HttpServletRequest req) throws UnsupportedEncodingException {
        for (String path : paths) {
            RedirectAttributesModelMap modelMap = new RedirectAttributesModelMap();
            logger.info("正在处理: {}", path);
            previewController.localPreview(path, modelMap, req);
        }
        logger.info("处理完成");
        return "success";
    }
}
