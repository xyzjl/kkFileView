package cn.keking.local;

import cn.keking.utils.PathUtils;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FileBrowserService {

    @Value(("${browse-root-path:./}"))
    private String browseRootPath;

    public List<FileModel> list(final String childPath, Boolean dirOnly) {
        boolean isRootPath = "".equals(childPath);
        String absRootPath;
        List<FileModel> fileModels = new ArrayList<>();
        try {
            absRootPath = new File(browseRootPath).getCanonicalPath().replaceAll("\\\\", "/");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!isRootPath) {
            fileModels.add(FileModel.createParentDir(absRootPath, childPath));
        }
        File dir = isRootPath ? new File(browseRootPath) : new File(browseRootPath, childPath);
        File[] files = dirOnly ? dir.listFiles(File::isDirectory) : dir.listFiles();
        if (files != null) {
            fileModels.addAll(Arrays.stream(files)
                    .map(file -> FileModel.createBy(file, absRootPath, childPath))
                    .collect(Collectors.toList()));
        }
        return fileModels;
    }

    public ResponseData getBrowsePathByRealPath(String realPath) {
        // 判断realpath是否是rootPath的子目录
        try {
            File realFile = new File(realPath);
            if(!realFile.exists()) {
                return ResponseData.error("目录或文件不存在");
            }
            String absRealPath = realFile.getCanonicalPath().replaceAll("\\\\", "/");
            String absRootPath = new File(browseRootPath).getCanonicalPath().replaceAll("\\\\", "/");
            if (!absRealPath.startsWith(absRootPath)) {
                throw new IllegalArgumentException("非法的文件访问");
            }
            String childPath = absRealPath.substring(absRootPath.length());
            return ResponseData.success(PathUtils.encode(childPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ResponseData getBrowsePathByChildPath(String childPath) {
        File file = new File(PathUtils.concat(browseRootPath, childPath));
        if(!file.exists()) {
            return ResponseData.error("目录或文件不存在");
        }
        return ResponseData.success(PathUtils.encode(childPath));
    }

    @Getter
    @Setter
    public static class FileModel {
        String name;
        String path;
        Boolean isFile;
        String realPath;
        Long fileSize;

        public static FileModel createParentDir(String absRootPath, String childPath) {
            childPath = childPath.substring(0, childPath.lastIndexOf("/"));
            FileModel fileModel = new FileModel();
            fileModel.setName("..");
            fileModel.setIsFile(false);
            fileModel.setRealPath(PathUtils.concat(absRootPath, childPath));
            fileModel.setPath(childPath.length() == 0 ? "*" : PathUtils.encode(childPath));
            return fileModel;
        }

        public static FileModel createBy(File file, String absRootPath, String childPath) {
            FileModel fileModel = new FileModel();
            fileModel.setName(file.getName());
            String filePath = PathUtils.encode(childPath + "/" + file.getName());
            fileModel.setPath(filePath);
            if (file.isFile()) {
                fileModel.setIsFile(true);
                fileModel.setFileSize(file.length());
            } else {
                fileModel.setIsFile(false);
            }
            fileModel.setRealPath(PathUtils.concat(absRootPath, childPath, file.getName()));
            return fileModel;
        }

    }
}
