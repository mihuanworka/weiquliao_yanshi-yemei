package com.ydd.yanshi.db.dao;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.ydd.yanshi.bean.message.ChatMessage;

import java.sql.SQLException;

public class ChatMessageDaoImpl extends BaseDaoImpl<ChatMessage, Integer> {
    public ChatMessageDaoImpl(ConnectionSource connectionSource, DatabaseTableConfig<ChatMessage> tableConfig) throws SQLException {
        super(connectionSource, tableConfig);
    }
}
