# 1.0.0-RC

## Feature
- Development environment to use docker-compose.yml
- Support the /metrics/find API to use at Grafana [#1](https://github.com/Dark0096/disthene-reader/issues/1) 
- Implement StatsService interface to be disable graphite stats service
- Change project build tool from maven to gradle

## Bug fix
- Fix /render api not supported about asterisk query [#3](https://github.com/Dark0096/disthene-reader/issues/3)
- Fix the duplication metric path to use /metrics/find API [#5](https://github.com/Dark0096/disthene-reader/issues/5)
- Add the elasticsearch client ping config to keep safely connection between elasticsearch cluster [#11](https://github.com/Dark0096/disthene-reader/issues/11) 
