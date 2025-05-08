package com.example.service;

import cn.hutool.core.lang.Dict;
import com.example.dao.ImSingleDao;
import com.example.entity.ImSingle;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ImSingleService {

    @Resource
    private ImSingleDao imSingleDao;

    public ImSingle add(ImSingle imSingle) {
        imSingleDao.insertSelective(imSingle);
        return imSingle;
    }
    public List<ImSingle> findByUsername(String fromUser, String toUser) {
        List<ImSingle> list = imSingleDao.findByUsername(fromUser, toUser);
        list.forEach(x -> {
            if (x.getTouser().equals(fromUser) && x.getFromuser().equals(toUser)) {
                x.setReaded(1);
                imSingleDao.updateByPrimaryKey(x);
            }
        });
        return list;
    }

    public Dict findUnReadNums(String toUsername) {
        List<ImSingle> list = imSingleDao.findByToUsername(toUsername);
        Map<String, List<ImSingle>> collect = list.stream().collect(Collectors.groupingBy(ImSingle::getFromuser));
        Dict dict = Dict.create();
        collect.forEach((k,v) -> dict.set(k, v.size()));
        return dict;
    }
}