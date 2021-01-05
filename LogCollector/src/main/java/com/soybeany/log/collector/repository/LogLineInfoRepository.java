package com.soybeany.log.collector.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/4
 */
public interface LogLineInfoRepository extends JpaRepository<LogLineInfo, Integer> {

    List<LogLineInfo> findByUidIsNullAndTimeBetweenOrderByTime(Date from, Date to);

    List<LogLineInfo> findByUid(String uid);

}
