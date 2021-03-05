package com.logan.demo.utils;

import com.logan.demo.config.SpringRedisConfig;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SentinelServersConfig;
import org.redisson.config.SingleServerConfig;

import java.util.concurrent.TimeUnit;

/*

1. dependency
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <version>3.14.0</version>
</dependency>

2. config redis info in application.yml
spring:
  redis:
    database: 0
    host: 127.0.0.1
    port: 6379
    password: ''
    timeout: 10000
    jedis:
      pool:
        max-active: 50
        max-idle: 10
        max-wait: 10000
        min-idle: 10
    # 红锁哨兵模式使用额外以下配置，上面的 host 配置不再起作用
    master-name: mymaster
    sentinel-addresses: 127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381
    # 红锁集群模式使用额外以下配置，上面的 红锁哨兵模式 和 host  配置不再起作用
    node-addresses: 127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381

3. setting this value to what you want to use
private static final int redisDeployType = RedisDeployType.SENTINEL;

 */


public class RedissonUtils {

    // 配置是否是哨兵模式，默认单节点
    private static final int redisDeployType = RedisDeployType.SENTINEL;
    private static RedissonClient redissonClient;

    private static final String uriHttpPrefix = "redis://";
    private static final String uriHttpsPrefix = "rediss://";

    /**
     * Jedis实例获取返回码
     */
    public static class RedisDeployType {
        public static final int SINGLE = 1;
        public static final int SENTINEL = 2;
        public static final int CLUSTER = 3;

    }


    public static RedissonClient redissonSingle() {
        // 如果不使用 spring， 则可在这里以其他方式获取配置，例如直接读取配置文件获取里面的值
        SpringRedisConfig conf = SpringBeanUtils.getBean(SpringRedisConfig.class);
        Config config = new Config();
        SingleServerConfig serverConfig = config.useSingleServer()
                .setAddress(uriHttpPrefix + conf.getHost() + ":" + conf.getPort())
                .setTimeout(conf.getTimeout())
                .setConnectionPoolSize(conf.getMaxActive())
                .setConnectionMinimumIdleSize(conf.getMinIdle());

        if (conf.getPassword() != null && !"".equals(conf.getPassword().trim())) {
            serverConfig.setPassword(conf.getPassword());
        }
        return Redisson.create(config);
    }


    public static RedissonClient redissonSentinel() {
        // 如果不使用 spring， 则可在这里以其他方式获取配置，例如直接读取配置文件获取里面的值
        SpringRedisConfig conf = SpringBeanUtils.getBean(SpringRedisConfig.class);
        Config config = new Config();
        // 127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381
        String[] split = conf.getSentinelAddresses().split(",");


        SentinelServersConfig serverConfig = config.useSentinelServers()
                .setMasterName(conf.getMasterName())
                .setTimeout(conf.getTimeout())
                .setMasterConnectionPoolSize(1000)
                .setSlaveConnectionPoolSize(1000);
        for (String s : split) {
            serverConfig.addSentinelAddress(uriHttpPrefix + s);
        }

        if (conf.getPassword() != null && !"".equals(conf.getPassword().trim())) {
            serverConfig.setPassword(conf.getPassword());
        }
        return Redisson.create(config);
    }

    // todo 还未通过集群验证
    public static RedissonClient redissonCluster() {
        // 如果不使用 spring， 则可在这里以其他方式获取配置，例如直接读取配置文件获取里面的值
        SpringRedisConfig conf = SpringBeanUtils.getBean(SpringRedisConfig.class);
        // 127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381
        String[] split = conf.getNodeAddresses().split(",");
        Config config = new Config();
        ClusterServersConfig serverConfig = config.useClusterServers();
        for (String s : split) {
            serverConfig.addNodeAddress(uriHttpPrefix + s);
        }

        if (conf.getPassword() != null && !"".equals(conf.getPassword().trim())) {
            serverConfig.setPassword(conf.getPassword());
        }
        return Redisson.create(config);
    }

    public static void intiClient() {
        if (redissonClient == null) {
            synchronized (RedissonUtils.class) {
                if (redissonClient == null) {
                    if (redisDeployType == RedisDeployType.SENTINEL) {
                        System.out.println("===== SENTINEL");
                        redissonClient = redissonSentinel();
                        return;
                    } else if (redisDeployType == RedisDeployType.CLUSTER) {
                        System.out.println("===== CLUSTER");
                        redissonClient = redissonCluster();
                        return;
                    }
                    System.out.println("===== SINGLE");
                    redissonClient = redissonSingle();
                }
            }
        }
    }

    public static RLock lock(String lockKey) {
        intiClient();
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        return lock;
    }


    public static RLock lock(String lockKey, int leaseTime) {
        intiClient();
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock(leaseTime, TimeUnit.SECONDS);
        return lock;
    }


    public static RLock lock(String lockKey, TimeUnit unit, int timeout) {
        intiClient();
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock(timeout, unit);
        return lock;
    }


    // 尝试加锁，最多等待 waitTime TimeUnit(秒)，上锁以后 leaseTime TimeUnit 自动解锁
    public static boolean tryLock(String lockKey, TimeUnit unit, int waitTime, int leaseTime) {
        intiClient();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            return false;
        }
    }


    public static void unlock(String lockKey) {
        intiClient();
        RLock lock = redissonClient.getLock(lockKey);
        lock.unlock();
    }


    public static void unlock(RLock lock) {
        intiClient();
        lock.unlock();
    }

    public static void setRedissonClient(RedissonClient redissonClient) {
        redissonClient = redissonClient;
    }


}
