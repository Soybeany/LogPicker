package com.soybeany.log.collector.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/4
 */
public interface LogLineInfoRepository extends JpaRepository<LogLineInfo, Integer> {

    List<LogLineInfo> findByTimeBetweenOrderByTime(String from, String to, Pageable pageable);

    List<LogLineInfo> findByUidOrderByTime(String uid);

}
