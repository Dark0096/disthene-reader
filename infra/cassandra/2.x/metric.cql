CREATE KEYSPACE metric WITH replication = {'class': 'NetworkTopologyStrategy', 'ap-northeast-2': '1'} AND durable_writes = true;

CREATE TABLE metric.metric (
  period int,
  rollup int,
  tenant text,
  path text,
  time bigint,
  data list<double>,
  PRIMARY KEY ((tenant, period, rollup, path), time)
) WITH
  bloom_filter_fp_chance=0.010000 AND
  caching='KEYS_ONLY' AND
  comment='' AND
  dclocal_read_repair_chance=0.000000 AND
  gc_grace_seconds=864000 AND
  index_interval=128 AND
  read_repair_chance=0.000000 AND
  replicate_on_write='true' AND
  populate_io_cache_on_flush='false' AND
  default_time_to_live=0 AND
  speculative_retry='NONE' AND
  memtable_flush_period_in_ms=0 AND
  compaction = {'sstable_size_in_mb': '640', 'class': 'org.apache.cassandra.db.compaction.LeveledCompactionStrategy'} AND
  compression={'sstable_compression': 'LZ4Compressor'};
