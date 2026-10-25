package com.example.dao;

import com.example.entity.Feedback;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

@Repository
public interface FeedbackDao extends Mapper<Feedback> {

    /**
     * 管理端分页列表（联表带出昵称与楼层名）。
     * 注意：自定义 SQL 一律显式列别名（项目未开驼峰映射）。
     */
    @Select("<script>select f.id, f.reporter_id as reporterId, f.room_id as roomId, " +
            "f.target_user_id as targetUserId, f.target_message_id as targetMessageId, " +
            "f.type, f.content, f.is_anonymous as isAnonymous, f.status, " +
            "f.handle_note as handleNote, f.handled_by as handledBy, " +
            "f.created_at as createdAt, f.handled_at as handledAt, " +
            "ru.nickname as reporterName, tu.nickname as targetName, cr.floor as roomName " +
            "from feedback f " +
            "left join user ru on f.reporter_id = ru.id " +
            "left join user tu on f.target_user_id = tu.id " +
            "left join chat_room cr on f.room_id = cr.id " +
            "<where><if test='status != null'>and f.status = #{status}</if></where> " +
            "order by f.id desc</script>")
    List<Feedback> findList(@Param("status") Integer status);

    @Select("select id, reporter_id as reporterId, room_id as roomId, target_user_id as targetUserId, " +
            "type, content, is_anonymous as isAnonymous, status, handle_note as handleNote, " +
            "created_at as createdAt, handled_at as handledAt " +
            "from feedback where reporter_id = #{reporterId} order by id desc limit 50")
    List<Feedback> findByReporterId(@Param("reporterId") Integer reporterId);

    @Select("select count(*) from feedback where status = 0")
    int countPending();
}
