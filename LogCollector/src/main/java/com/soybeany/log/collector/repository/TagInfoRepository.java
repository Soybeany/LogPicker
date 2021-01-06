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

    List<TagInfo> findByKeyAndTimeBetweenAndValueContaining(String key, Date from, Date to, String value, Pageable pageable);

    List<TagInfo> findByKeyAndValueContainingAndUidIn(String key, String value, Collection<String> uid);

    List<TagInfo> findByUid(String uid);

}
