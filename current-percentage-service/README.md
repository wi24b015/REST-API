# Current Percentage Service

This service listens to the `energy.updates` RabbitMQ queue, reads the matching hour from the `energy_usage` table and updates the `current_percentage` table.

## start sevice

bash:
mvn spring-boot:run


## calculation

community_depleted = community_used / community_produced * 100
grid_portion = grid_used / (community_used + grid_used) * 100
