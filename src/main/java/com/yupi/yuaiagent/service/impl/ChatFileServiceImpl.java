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

        try (Stream<Path> stream = Files.list(chatDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(this::getLastModifiedTime).reversed())
                    .map(this::toVO)
                    .toList();
        } catch (IOException e) {
            log.error("读取会话文件列表失败，userId={}, chatId={}", userId, chatId, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "读取会话文件列表失败");
        }
    }

    @Override
    public Resource loadFileAsResource(Long userId, Long chatId, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
        }

        Path chatDir = getChatDir(userId, chatId);
        Path filePath = chatDir.resolve(fileName).normalize();

        if (!filePath.startsWith(chatDir)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法文件路径");
        }

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件不存在");
        }

        return new FileSystemResource(filePath);
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

    private ChatFileVO toVO(Path path) {
        String fileName = path.getFileName().toString();
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
