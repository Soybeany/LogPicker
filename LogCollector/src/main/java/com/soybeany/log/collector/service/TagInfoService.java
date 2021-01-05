package com.soybeany.log.collector.service;

import com.soybeany.log.collector.model.QueryParam;
import com.soybeany.log.collector.repository.TagInfo;
import com.soybeany.log.collector.repository.TagInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/4
 */
public interface TagInfoService {

    List<String> getMatchedUidList(QueryParam queryParam);

}

@Service
class TagInfoServiceImpl implements TagInfoService {

    @Autowired
    private TagInfoRepository tagInfoRepository;

    @Override
    public List<String> getMatchedUidList(QueryParam param) {
        if (param.kvList.isEmpty()) {
            System.out.println("没有指定参数");
            return Collections.emptyList();
        }
        QueryParam.Kv firstKv = param.kvList.get(0);
        List<TagInfo> infoList = tagInfoRepository.findByKeyAndTimeBetweenAndValueContaining(firstKv.key, param.from, param.to, firstKv.value);
        for (int i = 1; i < param.kvList.size(); i++) {
            QueryParam.Kv kv = param.kvList.get(i);
            List<String> uids = toUidList(infoList);
            if (uids.isEmpty()) {
                return Collections.emptyList();
            }
            infoList = tagInfoRepository.findByKeyAndValueContainingAndUidIn(kv.key, kv.value, uids);
        }
        return toUidList(infoList);
    }

    private List<String> toUidList(List<TagInfo> list) {
        List<String> result = new LinkedList<>();
        for (TagInfo info : list) {
            result.add(info.uid);
        }
        return result;
    }

}
