package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.model.dto.FileRequest;
import com.yupi.yuaiagent.model.vo.ChatFileVO;
import com.yupi.yuaiagent.model.vo.LoginUserVO;
import com.yupi.yuaiagent.service.ChatFileService;
import com.yupi.yuaiagent.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/chat/file")
public class ChatFileController {

    @Resource
    private ChatFileService chatFileService;

    @Resource
    private UserService userService;

    /**
     * 查看某个对话生成的文件列表
     */
    @PostMapping("/list")
    public List<ChatFileVO> listFiles(@RequestBody FileRequest fileRequest,
                                      HttpServletRequest request) {
        LoginUserVO loginUser = userService.getLoginUser(request);
        return chatFileService.listFiles(
                loginUser.getUserId().longValue(),
                fileRequest.getChatId()
        );
    }

    /**
     * 下载某个对话生成的指定文件
     */
    @PostMapping("/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile(@RequestBody FileRequest fileRequest,
                                                                             HttpServletRequest request) {
        LoginUserVO loginUser = userService.getLoginUser(request);

        org.springframework.core.io.Resource resource = chatFileService.loadFileAsResource(
                loginUser.getUserId().longValue(),
                fileRequest.getChatId(),
                fileRequest.getFileName()
        );

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(fileRequest.getFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(resource);
    }
}
