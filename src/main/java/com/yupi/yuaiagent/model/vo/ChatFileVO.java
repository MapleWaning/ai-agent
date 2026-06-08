package com.yupi.yuaiagent.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatFileVO {

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小，单位：字节
     */
    private long size;

    /**
     * 格式化后的文件大小
     */
    private String sizeText;

    /**
     * 最后修改时间
     */
    private String lastModified;

    /**
     * 下载接口地址
     */
    private String downloadUrl;
}
