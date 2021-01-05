package com.soybeany.log.collector.repository;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Soybeany
 * @date 2021/1/4
 */
public interface LogLineInfoRepository extends JpaRepository<LogLineInfo, Integer> {
}
