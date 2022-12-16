package org.example.connect.reddis;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

@Slf4j
@Setter
@Getter
@ToString
public class RedisConnectionCell {
    private String url;
    private long relaxTime;
    private long timeOut;
    private Jedis jedis;


    public RedisConnectionCell(String url, long relaxTime) {
        super();
        this.url = url;
        this.relaxTime = relaxTime;

        try {
            jedis = new Jedis(url);
        }
        catch (JedisException e){
            log.error("connect unsuccessful to redis: ", e);
        }
    }

    public boolean isTimeOut() {
        if(System.currentTimeMillis() - relaxTime > timeOut) {
            return true;
        }
        return false;
    }

    public void close() throws Exception{
        try {
            jedis.close();
        } catch (Exception e) {
            log.warn("connection is closed: {0}", e);
        }
    }
    public boolean isClosed() throws Exception{
        return !jedis.isConnected();
    }
}
