package searchengine.config;

import org.hibernate.dialect.MySQL8Dialect;

public class DialectUTF8mb4 extends MySQL8Dialect {
    @Override
    public String getTableTypeString () {
        return "engine=innodb default charset=utf8mb4 collate utf8mb4_unicode_ci";
    }
}
