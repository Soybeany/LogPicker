package com.soybeany.log.collector.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/4
 */
public interface TagInfoRepository extends JpaRepository<TagInfo, Integer> {

    List<TagInfo> findByKeyAndTimeBetweenAndValueContainingOrderByTime(String key, Date from, Date to, String value, Pageable pageable);

    List<TagInfo> findByKeyAndValueContainingAndUidInOrderByTime(String key, String value, Collection<String> uid);

    List<TagInfo> findByUidAndThreadOrderByTime(String uid, String thread);

}
