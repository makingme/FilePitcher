package kr.uracle.ums.fpc.dbms;

import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class HikariCpDataSource implements DataSourceFactory{
	
	protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	
	private final HikariConfig config = new HikariConfig();
	
	private HikariDataSource dataSource = null;
		
	public static HikariCpDataSource getInstance() {
		return HikariCpInstance.instance;
	}
	
	private static class HikariCpInstance{
		private static HikariCpDataSource instance = new HikariCpDataSource();
	}
		
	@Override
	public void setProperties(Properties props) {
		
		String driverClassName = props.getProperty("driver"); 
		String jdbcUrl = props.getProperty("url");
		String userName = props.getProperty("username");
		String password = props.getProperty("password");
		
		String name = props.getProperty("name");
		
		if(StringUtils.isNotBlank(name)) {
			LOGGER.info("DataSourceName:{}", name);
			config.setPoolName(name);
		}
		
		LOGGER.info("driverClassName:{}", driverClassName);
		config.setDriverClassName(driverClassName);
		
		LOGGER.info("jdbcUrl:{}", jdbcUrl);
		config.setJdbcUrl(jdbcUrl);
		
		LOGGER.info("userName:{}", userName);
		config.setUsername(userName);
		
		LOGGER.debug("password:{}", password);
		config.setPassword(password);
		
		int idleTimeout = convertToInteger(props.getProperty("idletimeout", "0"));
		if(idleTimeout>0) {
			LOGGER.info("idleTimeout:{}", idleTimeout);
			config.setIdleTimeout(idleTimeout);
		}
		
		int minimumIdle = convertToInteger(props.getProperty("minimumIdle", "0"));
		if(minimumIdle>0) {
			LOGGER.info("minimumIdle:{}", minimumIdle);
			config.setMinimumIdle(minimumIdle);
		}
		
		int maximumPoolSize = convertToInteger(props.getProperty("maximumpoolsize", "0"));
		if(maximumPoolSize>0) {
			LOGGER.info("maximumPoolSize:{}", maximumPoolSize);
			config.setMaximumPoolSize(maximumPoolSize);
		}
		
		int maxLifetime = convertToInteger(props.getProperty("maxlifetime", "0"));
		if(maxLifetime>0) {
			LOGGER.info("maxLifetime:{}", maxLifetime);
			config.setMaxLifetime(maxLifetime);
		}

		int connectionTimeout = convertToInteger(props.getProperty("connectiontimeout", "0"));
		if(connectionTimeout>0) {
			LOGGER.info("connectionTimeout:{}", connectionTimeout);
			config.setConnectionTimeout(connectionTimeout);
		}
		
		int initializationFailTimeout = convertToInteger(props.getProperty("initializationfailtimeout", "0"));
		if(initializationFailTimeout>0) {
			LOGGER.info("initializationFailTimeout:{}", initializationFailTimeout);
			config.setInitializationFailTimeout(initializationFailTimeout);
		}
		
	}

	@Override
	public DataSource getDataSource() {
		if(dataSource == null || dataSource.isClosed()) {
			dataSource = new HikariDataSource(config);
		}
		return dataSource;
	}
	
	private int convertToInteger(String numberic) {
		if(StringUtils.isNotBlank(numberic)) return 0;
		
		numberic = numberic.replaceAll("\\D", "");
		
		if(StringUtils.isNotBlank(numberic)) return 0;
		
		return Integer.parseInt(numberic);
	}
}
