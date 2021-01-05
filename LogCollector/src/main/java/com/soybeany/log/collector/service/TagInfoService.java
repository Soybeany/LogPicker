package com.soybeany.log.collector.service;

import com.soybeany.log.collector.model.QueryParam;
import com.soybeany.log.collector.repository.TagInfo;
import com.soybeany.log.collector.repository.TagInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/4
 */
public interface TagInfoService extends QueryParam.ParamHandler {

    @NonNull
    List<String> getMatchedUidList(QueryParam queryParam);

}

@Service
class TagInfoServiceImpl implements TagInfoService {

    private static final String TAG_PREFIX = "tag";

    @Autowired
    private TagInfoRepository tagInfoRepository;

    @Override
    public boolean hasParams(QueryParam param) {
        return !param.getParams(TAG_PREFIX).isEmpty();
    }

    @Override
    public List<String> getMatchedUidList(QueryParam queryParam) {
        Map<String, String> params = queryParam.getParams(TAG_PREFIX);
        if (params.isEmpty()) {
            return Collections.emptyList();
        }
        Iterator<Map.Entry<String, String>> iterator = params.entrySet().iterator();
        Map.Entry<String, String> firstEntry = iterator.next();
        List<TagInfo> infoList = tagInfoRepository.findByKeyAndTimeBetweenAndValueContaining(firstEntry.getKey(), queryParam.getFrom(), queryParam.getTo(), firstEntry.getValue());
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            List<String> uids = toUidList(infoList);
            if (uids.isEmpty()) {
                return Collections.emptyList();
            }
            infoList = tagInfoRepository.findByKeyAndValueContainingAndUidIn(entry.getKey(), entry.getValue(), uids);
        }
        return toUidList(infoList);
    }

    @NonNull
    private List<String> toUidList(List<TagInfo> list) {
        List<String> result = new LinkedList<>();
        for (TagInfo info : list) {
            result.add(info.uid);
        }
        return result;
    }
}
