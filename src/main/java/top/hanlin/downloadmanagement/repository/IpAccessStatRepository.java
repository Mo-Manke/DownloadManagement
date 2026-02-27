package top.hanlin.downloadmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import top.hanlin.downloadmanagement.domain.IpAccessStat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public interface IpAccessStatRepository extends JpaRepository<IpAccessStat, Long> {

	// 单IP统计 - 总请求数和总下载流量
	@Query(value = "SELECT SUM(s.request_count) as totalRequests, SUM(s.download_bytes) as totalBytes " +
			"FROM ip_access_stat s WHERE s.ip = :ip AND s.occurred_at >= :startTime", nativeQuery = true)
	Map<String, Object> getIpTotalStats(@Param("ip") String ip, @Param("startTime") OffsetDateTime startTime);

	// 单IP按小时分布
	@Query(value = "SELECT DATE_FORMAT(CONVERT_TZ(s.occurred_at, '+00:00', '+08:00'), '%Y-%m-%d %H:00:00') as timeSlot, " +
			"SUM(s.request_count) as requests, SUM(s.download_bytes) as bytes " +
			"FROM ip_access_stat s WHERE s.occurred_at >= :startTime AND s.ip = :ip " +
			"GROUP BY DATE_FORMAT(CONVERT_TZ(s.occurred_at, '+00:00', '+08:00'), '%Y-%m-%d %H:00:00') ORDER BY timeSlot", nativeQuery = true)
	List<Map<String, Object>> getIpStatsByHour(@Param("ip") String ip, @Param("startTime") OffsetDateTime startTime);

	// 单IP按分钟分布
	@Query(value = "SELECT DATE_FORMAT(CONVERT_TZ(s.occurred_at, '+00:00', '+08:00'), '%Y-%m-%d %H:%i:00') as timeSlot, " +
			"SUM(s.request_count) as requests, SUM(s.download_bytes) as bytes " +
			"FROM ip_access_stat s WHERE s.occurred_at >= :startTime AND s.ip = :ip " +
			"GROUP BY DATE_FORMAT(CONVERT_TZ(s.occurred_at, '+00:00', '+08:00'), '%Y-%m-%d %H:%i:00') ORDER BY timeSlot", nativeQuery = true)
	List<Map<String, Object>> getIpStatsByMinute(@Param("ip") String ip, @Param("startTime") OffsetDateTime startTime);

	// 全局统计 - 总请求数和总下载流量
	@Query(value = "SELECT SUM(s.request_count) as totalRequests, SUM(s.download_bytes) as totalBytes " +
			"FROM ip_access_stat s WHERE s.occurred_at >= :startTime", nativeQuery = true)
	Map<String, Object> getGlobalTotalStats(@Param("startTime") OffsetDateTime startTime);

	// 全局按时间段分布（按小时）
	@Query(value = "SELECT DATE_FORMAT(CONVERT_TZ(s.occurred_at, '+00:00', '+08:00'), '%Y-%m-%d %H:00:00') as timeSlot, " +
			"SUM(s.request_count) as requests, SUM(s.download_bytes) as bytes " +
			"FROM ip_access_stat s WHERE s.occurred_at >= :startTime " +
			"GROUP BY DATE_FORMAT(CONVERT_TZ(s.occurred_at, '+00:00', '+08:00'), '%Y-%m-%d %H:00:00') ORDER BY timeSlot", nativeQuery = true)
	List<Map<String, Object>> getGlobalStatsByHour(@Param("startTime") OffsetDateTime startTime);

	// 全局按时间段分布（按天）
	@Query(value = "SELECT DATE_FORMAT(CONVERT_TZ(s.occurred_at, '+00:00', '+08:00'), '%Y-%m-%d') as timeSlot, " +
			"SUM(s.request_count) as requests, SUM(s.download_bytes) as bytes " +
			"FROM ip_access_stat s WHERE s.occurred_at >= :startTime " +
			"GROUP BY DATE_FORMAT(CONVERT_TZ(s.occurred_at, '+00:00', '+08:00'), '%Y-%m-%d') ORDER BY timeSlot", nativeQuery = true)
	List<Map<String, Object>> getGlobalStatsByDay(@Param("startTime") OffsetDateTime startTime);

	// 按IP分组统计（Top N）
	@Query(value = "SELECT s.ip, SUM(s.request_count) as requests, SUM(s.download_bytes) as bytes " +
			"FROM ip_access_stat s WHERE s.occurred_at >= :startTime " +
			"GROUP BY s.ip ORDER BY requests DESC", nativeQuery = true)
	List<Map<String, Object>> getTopIpsByRequests(@Param("startTime") OffsetDateTime startTime);
}

