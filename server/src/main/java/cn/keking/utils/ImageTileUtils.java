package cn.keking.utils;

import gov.nist.isg.archiver.DirectoryArchiver;
import gov.nist.isg.archiver.FilesArchiver;
import gov.nist.isg.pyramidio.BufferedImageReader;
import gov.nist.isg.pyramidio.PartialImageReader;
import gov.nist.isg.pyramidio.ScalablePyramidBuilder;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;

/**
 * 参考 <a href="https://github.com/usnistgov/pyramidio">源码</a>
 */
public class ImageTileUtils {
    /**
     * 瓦片大小
     */
    public static final int TILE_SIZE = 512;
    /**
     * 瓦片重叠区域大小
     */
    public static final int TILE_OVERLAP = 0;

    public static void createTiles(File inputImageFile, File dziFile) throws IOException {
        String baseName = FilenameUtils.getBaseName(dziFile.getName());
        ScalablePyramidBuilder spb = new ScalablePyramidBuilder(TILE_SIZE, TILE_OVERLAP, "png", "dzi");
        FilesArchiver archiver = new DirectoryArchiver(dziFile.getParentFile());
        PartialImageReader pir = new BufferedImageReader(inputImageFile);
        spb.buildPyramid(pir, baseName, archiver, 1);
    }

    public static void main(String[] args) throws IOException {
        File inputImageFile = new File("E:\\base\\工作资料\\2地质\\large_image\\anyrun.xyz.png");
        createTiles(inputImageFile, inputImageFile.getParentFile());
    }
}
