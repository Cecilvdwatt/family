Family API checker assignment.


Swagger API specification can be found in ./spec folder.

API spec was used to generate the controllers. Controller implementation can be found in _com.pink.family.assignment.api.controller.PersonsController_.

Single API endpoint was added: _/v1/people/check-existing-person_.

Project is largely structured as follows:

* _com.pink.family.assignment.api_
* * rest API code can be found here.
* _com.pink.family.assignment.database_
* * Hibernate (entities, repository, DAOs and mapper) can be found here.
* _com.pink.family.assignment.dto_
* * Data transfer objects largely used to abstract away from attached entities can be found here.
* _com.pink.family.assignment.properties_
* * only the caching properties
* _com.pink.family.assignment.services_
* * PersonService is the primary touch point between the API and the Database layer.
* * LoggingService. A very limited implementation for tracking requests IDs using MDC.
* _com.pink.family.assignment.util_
* * only contains the masking util class.

