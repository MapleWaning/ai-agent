package com.yupi.yuaiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yupi.yuaiagent.model.entity.ChatHistory;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {
}
