package com.redislabs.riot.db;

import com.redislabs.riot.AbstractImportCommand;
import com.redislabs.riot.ProcessorOptions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import javax.sql.DataSource;
import java.util.Map;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
@Command(name = "import", description = "Import from a database")
public class DatabaseImportCommand extends AbstractImportCommand<Map<String, Object>, Map<String, Object>> {

    @CommandLine.Parameters(arity = "1", description = "SQL SELECT statement", paramLabel = "SQL")
    private String sql;
    @Mixin
    private DataSourceOptions dataSourceOptions = new DataSourceOptions();
    @Mixin
    private DatabaseImportOptions importOptions = new DatabaseImportOptions();
    @CommandLine.ArgGroup(exclusive = false, heading = "Processor options%n")
    private ProcessorOptions processorOptions = new ProcessorOptions();

    @Override
    protected Flow flow(StepBuilderFactory stepBuilderFactory) throws Exception {
        log.debug("Creating data source: {}", dataSourceOptions);
        DataSource dataSource = dataSourceOptions.dataSource();
        String name = dataSource.getConnection().getMetaData().getDatabaseProductName();
        log.debug("Creating {} database reader: {}", name, importOptions);
        JdbcCursorItemReaderBuilder<Map<String, Object>> builder = new JdbcCursorItemReaderBuilder<>();
        builder.saveState(false);
        builder.dataSource(dataSource);
        if (importOptions.getFetchSize() != null) {
            builder.fetchSize(importOptions.getFetchSize());
        }
        if (importOptions.getMaxRows() != null) {
            builder.maxRows(importOptions.getMaxRows());
        }
        builder.name(name + "-database-reader");
        if (importOptions.getQueryTimeout() != null) {
            builder.queryTimeout(importOptions.getQueryTimeout());
        }
        builder.rowMapper(new ColumnMapRowMapper());
        builder.sql(sql);
        builder.useSharedExtendedConnection(importOptions.isUseSharedExtendedConnection());
        builder.verifyCursorPosition(importOptions.isVerifyCursorPosition());
        JdbcCursorItemReader<Map<String, Object>> reader = builder.build();
        reader.afterPropertiesSet();
        return flow(step(stepBuilderFactory.get(name + "-db-import-step"), "Importing from " + name, reader).build());
    }

    @Override
    protected ItemProcessor<Map<String, Object>, Map<String, Object>> processor() throws NoSuchMethodException {
        return processorOptions.processor(getRedisOptions());
    }
}
