package cn.keking.service.impl;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.ReturnResponse;
import cn.keking.service.FileHandlerService;
import cn.keking.utils.DownloadUtils;
import cn.keking.utils.ImageTileUtils;
import cn.keking.utils.KkFileUtils;
import gov.nist.isg.pyramidio.DeepZoomImageReader;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kl on 2018/1/17.
 * Content :图片文件处理
 */
@Service
public class PictureFilePreviewImpl extends CommonPreviewImpl {

    private static Logger logger = LoggerFactory.getLogger(PictureFilePreviewImpl.class);
    private static final long MIN_DZI_FILE_LENGTH = 5L * 1024 * 1024;

    private final FileHandlerService fileHandlerService;

    public PictureFilePreviewImpl(FileHandlerService fileHandlerService, OtherFilePreviewImpl otherFilePreview) {
        super(fileHandlerService, otherFilePreview);
        this.fileHandlerService = fileHandlerService;
    }

    @Override
    public String filePreviewHandle(String url, Model model, FileAttribute fileAttribute) {
        boolean isLocalPreview = BooleanUtils.toBoolean(String.valueOf(model.getAttribute("local")));
        String absolutePath;
        File dziFile;

        if (isLocalPreview) {
            absolutePath = (String) model.getAttribute("localPath");
            String[] pathArray = absolutePath.split(";");
            // 多图预览
            if (pathArray.length > 1) {
                List<String> imgUrls = (List<String>) model.getAttribute("imgUrls");
                model.addAttribute("currentUrl", imgUrls.get(0));
                model.addAttribute("imgUrls", imgUrls);
                return PICTURE_FILE_PREVIEW_PAGE;
            }
            String uniqueName = DigestUtils.md5Hex(absolutePath);
            // 只有大图才会用到DZI
            String dizSubDir = uniqueName.substring(0, 2) + "/" + uniqueName.substring(2, 4);
            String dziRelativePath = dizSubDir + "/" + uniqueName + ".dzi";
            dziFile = new File(ConfigConstants.getFileDir(), dziRelativePath);
            // 大图瓦片文件夹的依据
            model.addAttribute("dziBaseUrl", dizSubDir + "/" + uniqueName + "_files/");
            // 只有小图才会用到currentUrl
            model.addAttribute("currentUrl", url);
        } else {
            ReturnResponse<String> response = DownloadUtils.downLoad(fileAttribute, null);
            if (response.isFailure()) {
                return getOtherFilePreview().notSupportedFile(model, fileAttribute, response.getMsg());
            }
            absolutePath = response.getContent();
            // 只有大图才会用到DZI
            dziFile = new File(FilenameUtils.removeExtension(absolutePath) + ".dzi");
            // 只有小图才会用到currentUrl
            model.addAttribute("currentUrl", fileHandlerService.getRelativePath(absolutePath));
            // 大图瓦片文件夹的依据
            model.addAttribute("dziBaseUrl", String.format("./%s/", FilenameUtils.getBaseName(absolutePath)));
        }
        try {
            File imageFile = new File(absolutePath);
            // 5M以上的文件进行转化
            if (imageFile.length() > MIN_DZI_FILE_LENGTH) {
                if (!dziFile.exists()) {
                    logger.info("正在生成[{}]的瓦片[{}]", absolutePath, dziFile);
                    long l = System.currentTimeMillis();
                    ImageTileUtils.createTiles(imageFile, dziFile);
                    logger.info("瓦片生成完成，耗时{}ms", System.currentTimeMillis() - l);
                } else {
                    logger.info("图片[{}]超过5M，使用瓦片[{}]预览", absolutePath, dziFile);
                }
                DeepZoomImageReader reader = new DeepZoomImageReader(dziFile);
                model.addAttribute("currentWidth", String.valueOf(reader.getWidth()));
                model.addAttribute("currentHeight", String.valueOf(reader.getHeight()));
                model.addAttribute("currentType", "tile");
            } else {
                model.addAttribute("currentType", "image");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return BIGPICTURE_FILE_PREVIEW_PAGE;
    }
}
