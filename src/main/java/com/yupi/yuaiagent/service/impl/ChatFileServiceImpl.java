package com.yupi.yuaiagent.service.impl;

import com.yupi.yuaiagent.common.ErrorCode;
import com.yupi.yuaiagent.constant.FileConstant;
import com.yupi.yuaiagent.exception.BusinessException;
import com.yupi.yuaiagent.model.vo.ChatFileVO;
import com.yupi.yuaiagent.service.ChatFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
public class ChatFileServiceImpl implements ChatFileService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    @Override
    public List<ChatFileVO> listFiles(Long userId, Long chatId) {
        Path chatDir = getChatDir(userId, chatId);

        if (!Files.exists(chatDir) || !Files.isDirectory(chatDir)) {
            return List.of();
        }

        // Python 工具会把 PDF 等文件写入 pdf/、download/ 等子目录，需递归扫描
        try (Stream<Path> stream = Files.walk(chatDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(this::getLastModifiedTime).reversed())
                    .map(path -> toVO(chatDir, path))
                    .toList();
        } catch (IOException e) {
            log.error("读取会话文件列表失败，userId={}, chatId={}", userId, chatId, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "读取会话文件列表失败");
        }
    }

    @Override
    public Resource loadFileAsResource(Long userId, Long chatId, String fileName) {
        Path chatDir = getChatDir(userId, chatId);
        Path filePath = resolveFilePath(chatDir, fileName);
        return new FileSystemResource(filePath);
    }

    @Override
    public void deleteFile(Long userId, Long chatId, String fileName) {
        Path chatDir = getChatDir(userId, chatId);
        Path filePath = resolveFilePath(chatDir, fileName);
        try {
            Files.delete(filePath);
        } catch (IOException e) {
            log.error("删除文件失败，userId={}, chatId={}, fileName={}", userId, chatId, fileName, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除文件失败");
        }
    }

    private Path resolveFilePath(Path chatDir, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
        }

        Path filePath = chatDir.resolve(fileName).normalize();

        if (!filePath.startsWith(chatDir)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法文件路径");
        }

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件不存在");
        }

        return filePath;
    }

    /**
     * 获取会话文件目录：项目根目录/tmp/{userId}/{chatId}
     */
    private Path getChatDir(Long userId, Long chatId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (chatId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "chatId 不能为空");
        }

        Path tmpRoot = Paths.get(FileConstant.FILE_SAVE_DIR)
                .toAbsolutePath()
                .normalize();

        Path chatDir = tmpRoot
                .resolve(String.valueOf(userId))
                .resolve(String.valueOf(chatId))
                .normalize();

        if (!chatDir.startsWith(tmpRoot)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法目录路径");
        }

        return chatDir;
    }

    private ChatFileVO toVO(Path chatDir, Path path) {
        // 使用相对路径（如 pdf/xxx.pdf），与 download/delete 解析逻辑一致
        String fileName = chatDir.relativize(path).toString().replace('\\', '/');
        long size = getFileSize(path);

        return ChatFileVO.builder()
                .fileName(fileName)
                .size(size)
                .sizeText(formatSize(size))
                .lastModified(DATE_TIME_FORMATTER.format(getLastModifiedTime(path)))
                .downloadUrl("/api/chat/file/download")
                .build();
    }

    private long getFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0L;
        }
    }

    private Instant getLastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException e) {
            return Instant.EPOCH;
        }
    }

    private String formatSize(long size) {
        if (size < 1024) {
            return size + " B";
        }
        if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        }
        return String.format("%.1f MB", size / 1024.0 / 1024.0);
    }
}
