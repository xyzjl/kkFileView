package cn.keking.local;

import cn.keking.utils.PathUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class FileBrowserAction {
    private final FileBrowserService fileBrowserService;

    public FileBrowserAction(FileBrowserService fileBrowserService) {
        this.fileBrowserService = fileBrowserService;
    }

    @GetMapping("/browser/{path:.+}")
    public List<FileBrowserService.FileModel> browser(
            @PathVariable String path,
            @RequestParam(value = "dirOnly", required = false, defaultValue = "false") Boolean dirOnly) {
        if ("*".equals(path)) {
            return fileBrowserService.list("", dirOnly);
        }
        String filePath = PathUtils.decode(path);
        return fileBrowserService.list(filePath, dirOnly);
    }

    @GetMapping("/browser/getBrowsePathByRealPath")
    public ResponseData getBrowsePathByRealPath(String realPath) {
        return fileBrowserService.getBrowsePathByRealPath(realPath);
    }

    @GetMapping("/browser/getBrowsePathByChildPath")
    public ResponseData getBrowsePathByChildPath(String childPath) {
        return fileBrowserService.getBrowsePathByChildPath(childPath);
    }
}
