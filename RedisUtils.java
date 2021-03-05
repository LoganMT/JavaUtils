package com.logan.demo.utils;

import com.logan.demo.config.SpringRedisConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.SetParams;

import java.util.*;

/*
 收集汇总来的 jedis 工具类

1. dependency
<!-- https://mvnrepository.com/artifact/redis.clients/jedis -->
<dependency>
<groupId>redis.clients</groupId>
<artifactId>jedis</artifactId>
<version>3.5.1</version>
</dependency>

2. config redis in application.yml
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



 */


public class RedisUtils {
    private static final Logger log = LoggerFactory.getLogger(RedisUtils.class);

    private static JedisPool jedisPool = null;

    /**
     * Jedis实例获取返回码
     */
    public static class JedisStatus {
        public static final long FAIL_LONG = -1L;
        public static final String FAIL_STRING = "-1";
        private static final String LOCK_SUCCESS = "OK";
        private static final long TRY_LOCK_INTERVAL = 50L; // 间隔这么久重试一次
    }


    /**
     * 获取分布式锁,可等待重试
     *
     * @param lockKey    加锁的key
     * @param requestId  锁请求ID
     * @param expireTime 锁失效,毫秒
     * @param tryTime    重试时间,毫秒
     * @return 是否获取成功
     */
    public static boolean tryLock(String lockKey, String requestId, int expireTime, long tryTime) {
        Jedis jedis = getJedis();
        try {
            long startTry = System.currentTimeMillis();        // 开始重试时间
            long nextTry = 0L;                                // 下次重试时间

            while (true) {
                if (System.currentTimeMillis() < nextTry) {
                    continue;
                }
                SetParams setParams = new SetParams();
                setParams.nx().px(expireTime);
                String result = jedis.set(lockKey, requestId, setParams);
                if (JedisStatus.LOCK_SUCCESS.equalsIgnoreCase(result)) {
                    return true;
                }
                if ((System.currentTimeMillis() - startTry) > tryTime) {
                    return false;
                }
                nextTry = System.currentTimeMillis() + JedisStatus.TRY_LOCK_INTERVAL;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            returnResource(jedis);
        }
        return false;
    }


    /**
     * 获取分布式锁
     *
     * @param lockKey    加锁的key
     * @param requestId  锁请求ID
     * @param expireTime 锁失效,毫秒
     * @return 是否获取成功
     */
    public static boolean lock(String lockKey, String requestId, int expireTime) {
        Jedis jedis = getJedis();
        try {
            SetParams setParams = new SetParams();
            setParams.nx().px(expireTime);
            String result = jedis.set(lockKey, requestId, setParams);
            if (JedisStatus.LOCK_SUCCESS.equals(result)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            returnResource(jedis);
        }
        return false;
    }


    /**
     * 释放分布式锁
     *
     * @param lockKey   加锁的key
     * @param requestId 锁请求ID
     * @return 是否释放成功
     */
    public static boolean unLock(String lockKey, String requestId) {
        Jedis jedis = getJedis();
        try {
            //利用LUA脚本解锁一步到位,保持操作的原子性,具体说明请参考redis特性。
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Object result = jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
            long rs = Long.parseLong(result.toString());
            if (rs > 0) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            returnResource(jedis);
        }
        return false;
    }


    private static void initialPool() {
        try {
            // 如果不使用 spring， 则可在这里以其他方式获取配置，例如直接读取配置文件获取里面的值
            SpringRedisConfig conf = SpringBeanUtils.getBean(SpringRedisConfig.class);

            JedisPoolConfig config = new JedisPoolConfig();
            //最大连接数，如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)。
            config.setMaxTotal(conf.getMaxActive());
            //最大空闲数，控制一个pool最多有多少个状态为idle(空闲的)的jedis实例，默认值也是8。
            config.setMaxIdle(conf.getMaxIdle());
            //最小空闲数
            config.setMinIdle(conf.getMinIdle());
            //是否在从池中取出连接前进行检验，如果检验失败，则从池中去除连接并尝试取出另一个
            config.setTestOnBorrow(true);
            //在return给pool时，是否提前进行validate操作
            config.setTestOnReturn(true);
            //在空闲时检查有效性，默认false
            config.setTestWhileIdle(true);
            //表示一个对象至少停留在idle状态的最短时间，然后才能被idle object evitor扫描并驱逐；
            //这一项只有在timeBetweenEvictionRunsMillis大于0时才有意义
            config.setMinEvictableIdleTimeMillis(30000);
            //表示idle object evitor两次扫描之间要sleep的毫秒数
            config.setTimeBetweenEvictionRunsMillis(60000);
            //表示idle object evitor每次扫描的最多的对象数
            config.setNumTestsPerEvictionRun(1000);
            //等待可用连接的最大时间，单位毫秒，默认值为-1，表示永不超时。如果超过等待时间，则直接抛出JedisConnectionException；
            int MAX_WAIT = 15 * 1000;
            config.setMaxWaitMillis(MAX_WAIT);

            JedisPoolConfig cg = new JedisPoolConfig();
            cg.setMaxTotal(config.getMaxTotal());
            cg.setMaxIdle(config.getMaxIdle());
            cg.setMinIdle(config.getMinIdle());
            cg.setMaxWaitMillis(config.getMaxWaitMillis());
            cg.setTestOnBorrow(config.getTestOnBorrow());
            cg.setTestOnReturn(config.getTestOnReturn());
            cg.setTestWhileIdle(config.getTestWhileIdle());
            cg.setMinEvictableIdleTimeMillis(config.getMinEvictableIdleTimeMillis());
            cg.setTimeBetweenEvictionRunsMillis(config.getTimeBetweenEvictionRunsMillis());
            cg.setNumTestsPerEvictionRun(config.getNumTestsPerEvictionRun());

            //超时时间
            int TIMEOUT = 10 * 1000;
            if (conf.getPassword() != null && !"".equals(conf.getPassword().trim())) {
                jedisPool = new JedisPool(config, conf.getHost(), conf.getPort(), TIMEOUT, conf.getPassword());
            } else {
                jedisPool = new JedisPool(config, conf.getHost(), conf.getPort(), TIMEOUT);
            }
        } catch (Exception e) {
            if (jedisPool != null) {
                jedisPool.close();
            }
            log.error("初始化Redis连接池失败", e);
        }
    }

    /*
     * 初始化Redis连接池
     */
    static {
        initialPool();
    }

    /**
     * 在多线程环境同步初始化
     */
    private static synchronized void poolInit() {
        if (jedisPool == null) {
            initialPool();
        }
    }

    /**
     * 同步获取Jedis实例
     */
    public static Jedis getJedis() {
        if (jedisPool == null) {
            poolInit();
        }

        Jedis jedis = null;
        try {
            if (jedisPool != null) {
                jedis = jedisPool.getResource();
            }
        } catch (Exception e) {
            log.error("同步获取Jedis实例失败" + e.getMessage(), e);
            returnBrokenResource(jedis);
        }

        return jedis;
    }

    /**
     * 释放jedis资源
     */
    public static void returnResource(final Jedis jedis) {
        if (jedis != null && jedisPool != null) {
            jedis.close();
        }
    }


    public static void returnBrokenResource(final Jedis jedis) {
        if (jedis != null && jedisPool != null) {
            jedis.close();
        }
    }

    /**
     * 设置值
     *
     * @return -5：Jedis实例获取失败<br/>OK：操作成功<br/>null：操作失败
     */
    public static String set(String key, String value) {
        Jedis jedis = getJedis();
        if (jedis == null) {
            return JedisStatus.FAIL_STRING;
        }

        String result = null;
        try {
            result = jedis.set(key, value);
        } catch (Exception e) {
            log.error("设置值失败：" + e.getMessage(), e);
            returnBrokenResource(jedis);
        } finally {
            returnResource(jedis);
        }

        return result;
    }

    /**
     * 设置值
     *
     * @param expire 过期时间，单位：秒
     * @return -5：Jedis实例获取失败<br/>OK：操作成功<br/>null：操作失败
     */
    public static String set(String key, String value, int expire) {
        Jedis jedis = getJedis();
        if (jedis == null) {
            return JedisStatus.FAIL_STRING;
        }

        String result = null;
        try {
            result = jedis.set(key, value);
            jedis.expire(key, expire);
        } catch (Exception e) {
            log.error("设置值失败：" + e.getMessage(), e);
            returnBrokenResource(jedis);
        } finally {
            returnResource(jedis);
        }

        return result;
    }

    /**
     * 获取值
     */
    public static String get(String key) {
        Jedis jedis = getJedis();
        if (jedis == null) {
            return JedisStatus.FAIL_STRING;
        }

        String result = null;
        try {
            result = jedis.get(key);
        } catch (Exception e) {
            log.error("获取值失败：" + e.getMessage(), e);
            returnBrokenResource(jedis);
        } finally {
            returnResource(jedis);
        }

        return result;
    }

    /**
     * 设置key的过期时间
     */
    public static long expire(String key, int seconds) {
        Jedis jedis = getJedis();
        if (jedis == null) {
            return JedisStatus.FAIL_LONG;
        }

        long result = 0L;
        try {
            result = jedis.expire(key, seconds);
        } catch (Exception e) {
            log.error(String.format("设置key=%s的过期时间失败：" + e.getMessage(), key), e);
            returnBrokenResource(jedis);
        } finally {
            returnResource(jedis);
        }

        return result;
    }

    /**
     * 判断key是否存在
     */
    public static boolean exists(String key) {
        Jedis jedis = getJedis();
        if (jedis == null) {
            log.warn("Jedis实例获取为空");
            return false;
        }

        boolean result = false;
        try {
            result = jedis.exists(key);
        } catch (Exception e) {
            log.error(String.format("判断key=%s是否存在失败：" + e.getMessage(), key), e);
            returnBrokenResource(jedis);
        } finally {
            returnResource(jedis);
        }

        return result;
    }

    /**
     * 删除key
     *
     * @return -5：Jedis实例获取失败，1：成功，0：失败
     */
    public static long del(String... keys) {
        Jedis jedis = getJedis();
        if (jedis == null) {
            return JedisStatus.FAIL_LONG;
        }

        long result = JedisStatus.FAIL_LONG;
        try {
            result = jedis.del(keys);
        } catch (Exception e) {
            log.error(String.format("删除key=%s失败：" + e.getMessage(), (Object) keys), e);
            returnBrokenResource(jedis);
        } finally {
            returnResource(jedis);
        }

        return result;
    }

    /**
     * set if not exists，若key已存在，则setnx不做任何操作
     *
     * @param value key已存在，1：key赋值成功
     */
    public static long setnx(String key, String value) {
        long result = JedisStatus.FAIL_LONG;

        Jedis jedis = getJedis();
        if (jedis == null) {
            return result;
        }

        try {
            result = jedis.setnx(key, value);
        } catch (Exception e) {
            log.error("设置值失败：" + e.getMessage(), e);
            returnBrokenResource(jedis);
        } finally {
            returnResource(jedis);
        }

        return result;
    }

    /**
     * set if not exists，若key已存在，则setnx不做任何操作
     *
     * @param value  key已存在，1：key赋值成功
     * @param expire 过期时间，单位：秒
     */
    public static long setnx(String key, String value, int expire) {
        long result = JedisStatus.FAIL_LONG;

        Jedis jedis = getJedis();
        if (jedis == null) {
            return result;
        }

        try {
            result = jedis.setnx(key, value);
            jedis.expire(key, expire);
        } catch (Exception e) {
            log.error("设置值失败：" + e.getMessage(), e);
            returnBrokenResource(jedis);
        } finally {
            returnResource(jedis);
        }

        return result;
    }

    /**
     * 在列表key的头部插入元素
     */
    public static long lpush(String key, String... values) {
        long result = JedisStatus.FAIL_LONG;

        Jedis jedis = getJedis();
        if (jedis == null) {
            return result;
        }

        try {
            result = jedis.lpush(key, values);
        } catch (Exception e) {
            log.error("在列表key的头部插入元素失败：" + e.getMessage(), e);
            returnBrokenResource(jedis);
        } finally {
            returnResource(jedis);
        }

        return result;
    }

    /**
     * 在列表key的尾部插入元素
     */
    public static long rpush(String key, String... values) {
        long result = JedisStatus.FAIL_LONG;

        Jedis jedis = getJedis();
        if (jedis == null) {
            return result;
        }

        try {
            result = jedis.rpush(key, values);
        } catch (Exception e) {
            log.error("在列表key的尾部插入元素失败：" + e.getMessage(), e);
            returnBrokenResource(jedis);
        } finally {
            returnResource(jedis);
        }

        return result;
    }

    /**
     * 返回存储在key列表的特定元素
     *
     * @param start 开始索引，索引从0开始，0表示第一个元素，1表示第二个元素
     * @param end   结束索引，-1表示最后一个元素，-2表示倒数第二个元素
     */
    public static List<String> lrange(String key, long start, long end) {
        Jedis jedis = getJedis();
        if (jedis == null) {
            return null;
        }

        List<String> result = null;
        try {
            result = jedis.lrange(key, start, end);
        } catch (Exception e) {
            log.error("查询列表元素失败：" + e.getMessage(), e);
            returnBrokenResource(jedis);
        } finally {
            returnResource(jedis);
        }

        return result;
    }

    /**
     * 获取列表长度
     */
    public static long llen(String key) {
        Jedis jedis = getJedis();
        if (jedis == null) {
            return JedisStatus.FAIL_LONG;
        }

        long result = 0;
        try {
            result = jedis.llen(key);
        } catch (Exception e) {
            log.error("获取列表长度失败：" + e.getMessage(), e);
            returnBrokenResource(jedis);
        } finally {
            returnResource(jedis);
        }

        return result;
    }

    /**
     * 移除等于value的元素
     * 当count>0时，从表头开始查找，移除count个；
     * 当count=0时，从表头开始查找，移除所有等于value的；
     * 当count<0时，从表尾开始查找，移除count个
     */
    public static long lrem(String key, long count, String value) {
        Jedis jedis = getJedis();
        if (jedis == null) {
            return JedisStatus.FAIL_LONG;
        }

        long result = 0;
        try {
            result = jedis.lrem(key, count, value);
        } catch (Exception e) {
            log.error("获取列表长度失败：" + e.getMessage(), e);
            returnBrokenResource(jedis);
        } finally {
            returnResource(jedis);
        }

        return result;
    }

    /**
     * 对列表进行修剪
     */
    public static String ltrim(String key, long start, long end) {
        Jedis jedis = getJedis();
        if (jedis == null) {
            return JedisStatus.FAIL_STRING;
        }

        String result = "";
        try {
            result = jedis.ltrim(key, start, end);
        } catch (Exception e) {
            log.error("获取列表长度失败：" + e.getMessage(), e);
            returnBrokenResource(jedis);
        } finally {
            returnResource(jedis);
        }

        return result;
    }


    /**
     * 缓存Map赋值
     */
    public static long hset(String key, String field, String value) {
        Jedis jedis = getJedis();
        if (jedis == null) {
            return JedisStatus.FAIL_LONG;
        }

        long result = 0L;
        try {
            result = jedis.hset(key, field, value);
        } catch (Exception e) {
            log.error("缓存Map赋值失败：" + e.getMessage(), e);
            returnBrokenResource(jedis);
        } finally {
            returnResource(jedis);
        }

        return result;
    }


    /**
     * 获取缓存的Map值
     */
    public static String hget(String key, String field) {
        Jedis jedis = getJedis();
        if (jedis == null) {
            return null;
        }

        String result = null;
        try {
            result = jedis.hget(key, field);
        } catch (Exception e) {
            log.error("获取缓存的Map值失败：" + e.getMessage(), e);
            returnBrokenResource(jedis);
        } finally {
            returnResource(jedis);
        }

        return result;
    }

    /**
     * 获取map所有的字段和值
     */
    public static Map<String, String> hgetAll(String key) {
        Map<String, String> map = new HashMap<String, String>();

        Jedis jedis = getJedis();
        if (jedis == null) {
            log.warn("Jedis实例获取为空");
            return map;
        }

        try {
            map = jedis.hgetAll(key);
        } catch (Exception e) {
            log.error("获取map所有的字段和值失败：" + e.getMessage(), e);
            returnBrokenResource(jedis);
        } finally {
            returnResource(jedis);
        }

        return map;
    }

    /**
     * 查看哈希表 key 中，指定的field字段是否存在。
     */
    public static Boolean hexists(String key, String field) {
        Jedis jedis = getJedis();
        if (jedis == null) {
            log.warn("Jedis实例获取为空");
            return null;
        }

        try {
            return jedis.hexists(key, field);
        } catch (Exception e) {
            log.error("查看哈希表field字段是否存在失败：" + e.getMessage(), e);
            returnBrokenResource(jedis);
        } finally {
            returnResource(jedis);
        }

        return null;
    }

    /**
     * 获取所有哈希表中的字段
     */
    public static Set<String> hkeys(String key) {
        Set<String> set = new HashSet<>();
        Jedis jedis = getJedis();
        if (jedis == null) {
            log.warn("Jedis实例获取为空");
            return set;
        }

        try {
            return jedis.hkeys(key);
        } catch (Exception e) {
            log.error("获取所有哈希表中的字段失败：" + e.getMessage(), e);
            returnBrokenResource(jedis);
        } finally {
            returnResource(jedis);
        }

        return null;
    }

    /**
     * 获取所有哈希表中的值
     */
    public static List<String> hvals(String key) {
        List<String> list = new ArrayList<>();
        Jedis jedis = getJedis();
        if (jedis == null) {
            log.warn("Jedis实例获取为空");
            return list;
        }

        try {
            return jedis.hvals(key);
        } catch (Exception e) {
            log.error("获取所有哈希表中的值失败：" + e.getMessage(), e);
            returnBrokenResource(jedis);
        } finally {
            returnResource(jedis);
        }

        return null;
    }

    /**
     * 从哈希表 key 中删除指定的 field
     */
    public static long hdel(String key, String... fields) {
        Jedis jedis = getJedis();
        if (jedis == null) {
            log.warn("Jedis实例获取为空");
            return JedisStatus.FAIL_LONG;
        }

        try {
            return jedis.hdel(key, fields);
        } catch (Exception e) {
            log.error("map删除指定的field失败：" + e.getMessage(), e);
            returnBrokenResource(jedis);
        } finally {
            returnResource(jedis);
        }

        return 0;
    }

    public static Set<String> keys(String pattern) {
        Set<String> keyList = new HashSet<>();
        Jedis jedis = getJedis();
        if (jedis == null) {
            log.warn("Jedis实例获取为空");
            return keyList;
        }

        try {
            keyList = jedis.keys(pattern);
        } catch (Exception e) {
            log.error("操作keys失败：" + e.getMessage(), e);
            returnBrokenResource(jedis);
        } finally {
            returnResource(jedis);
        }
        return keyList;
    }


}
