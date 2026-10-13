# AEM Training Assignment – IFB Team Hub

AEM as a Cloud Service project (Maven archetype, Java 11 build) implementing the training assignment.

| Task | What was built | Where |
|---|---|---|
| **Task 1** – Template & Experience Fragments | Editable template **IFB Landing Page** with our own Header and Footer Experience Fragments (custom `siteheader` / `sitefooter` components, not Core defaults). The Home and Our Team pages are created from it. | `ui.content/.../conf/ifb/settings/wcm/templates/ifb-landing-page`<br>`ui.content/.../content/experience-fragments/ifb/us/en/site/ifb-header`, `ifb-footer`<br>`ui.apps/.../components/siteheader`, `sitefooter` |
| **Task 2** – Team Members component | Multifield dialog (name, role, photo, bio), own clientlib: role filter buttons and flip cards with bio. Authored on the Our Team page. | `ui.apps/.../components/teammembers`<br>`core/.../models/TeamMembers.java`, `TeamMember.java` |
| **Bonus** – City Weather component | Author enters a city; the server fetches current weather (OpenWeatherMap when an API key is configured, keyless Open-Meteo otherwise) with a 10-minute cache. Clientlib adds a °C/°F toggle and a Refresh button backed by a `.weather.json` servlet. | `ui.apps/.../components/cityweather`<br>`core/.../services/impl/WeatherServiceImpl.java`<br>`core/.../models/CityWeather.java`<br>`core/.../servlets/CityWeatherServlet.java` |

### Run it

```
mvn clean install -PautoInstallSinglePackage -pl '!ui.tests'
```

Then open (author, admin/admin):

- Home: http://localhost:4502/content/ifb/us/en.html
- Our Team: http://localhost:4502/content/ifb/us/en/our-team.html
- Template: http://localhost:4502/editor.html/conf/ifb/settings/wcm/templates/ifb-landing-page/structure.html

`ui.tests` (Cypress) is excluded because its `npm ci` does not run on Windows; Cloud Manager runs it in its own pipeline.

**OpenWeatherMap key (optional):** set `apiKey` in `ui.config/.../com.ifb.aem.core.core.services.impl.WeatherServiceImpl.cfg.json`, or the `OPENWEATHER_API_KEY` secret environment variable in Cloud Manager. Without a key the component uses Open-Meteo.

---

# Sample AEM project template

This is a project template for AEM-based applications. It is intended as a best-practice set of examples as well as a potential starting point to develop your own functionality.

## Modules

The main parts of the template are:

* [core:](core/README.md) Java bundle containing all core functionality like OSGi services, listeners or schedulers, as well as component-related Java code such as servlets or request filters.
* [it.tests:](it.tests/README.md) Java based integration tests
* [ui.apps:](ui.apps/README.md) contains the /apps (and /etc) parts of the project, ie JS&CSS clientlibs, components, and templates
* [ui.content:](ui.content/README.md) contains sample content using the components from the ui.apps
* ui.config: contains runmode specific OSGi configs for the project
* [ui.frontend:](ui.frontend.general/README.md) an optional dedicated front-end build mechanism (general Webpack project)
* [ui.tests:](ui.tests/README.md) Cypress based UI tests (for other frameworks check [aem-test-samples](https://github.com/adobe/aem-test-samples) repository
* all: a single content package that embeds all of the compiled modules (bundles and content packages) including any vendor dependencies
* analyse: this module runs analysis on the project which provides additional validation for deploying into AEMaaCS

## How to build

To build all the modules run in the project root directory the following command with Maven 3:

    mvn clean install

To build all the modules and deploy the `all` package to a local instance of AEM, run in the project root directory the following command:

    mvn clean install -PautoInstallSinglePackage

Or to deploy it to a publish instance, run

    mvn clean install -PautoInstallSinglePackagePublish

Or alternatively

    mvn clean install -PautoInstallSinglePackage -Daem.port=4503

Or to deploy only the bundle to the author, run

    mvn clean install -PautoInstallBundle

Or to deploy only a single content package, run in the sub-module directory (i.e `ui.apps`)

    mvn clean install -PautoInstallPackage

## Documentation

The build process also generates documentation in the form of README.md files in each module directory for easy reference. Depending on the options you select at build time, the content may be customized to your project.

## Testing

There are three levels of testing contained in the project:

### Unit tests

This show-cases classic unit testing of the code contained in the bundle. To
test, execute:

    mvn clean test

### Integration tests

This allows running integration tests that exercise the capabilities of AEM via
HTTP calls to its API. To run the integration tests, run:

    mvn clean verify -Plocal

Test classes must be saved in the `src/main/java` directory (or any of its
subdirectories), and must be contained in files matching the pattern `*IT.java`.

The configuration provides sensible defaults for a typical local installation of
AEM. If you want to point the integration tests to different AEM author and
publish instances, you can use the following system properties via Maven's `-D`
flag.

| Property              | Description                                         | Default value           |
|-----------------------|-----------------------------------------------------|-------------------------|
| `it.author.url`       | URL of the author instance                          | `http://localhost:4502` |
| `it.author.user`      | Admin user for the author instance                  | `admin`                 |
| `it.author.password`  | Password of the admin user for the author instance  | `admin`                 |
| `it.publish.url`      | URL of the publish instance                         | `http://localhost:4503` |
| `it.publish.user`     | Admin user for the publish instance                 | `admin`                 |
| `it.publish.password` | Password of the admin user for the publish instance | `admin`                 |

The integration tests in this archetype use the [AEM Testing
Clients](https://github.com/adobe/aem-testing-clients) and showcase some
recommended [best
practices](https://github.com/adobe/aem-testing-clients/wiki/Best-practices) to
be put in use when writing integration tests for AEM.

## Static Analysis

The `analyse` module performs static analysis on the project for deploying into AEMaaCS. It is automatically
run when executing

    mvn clean install

from the project root directory. Additional information about this analysis and how to further configure it
can be found here https://github.com/adobe/aemanalyser-maven-plugin

### UI tests

They will test the UI layer of your AEM application using Cypress framework.

Check README file in `ui.tests` module for more details.

Examples of UI tests in different frameworks can be found here: https://github.com/adobe/aem-test-samples

## ClientLibs

The frontend module is made available using an [AEM ClientLib](https://helpx.adobe.com/experience-manager/6-5/sites/developing/using/clientlibs.html). When executing the NPM build script, the app is built and the [`aem-clientlib-generator`](https://github.com/wcm-io-frontend/aem-clientlib-generator) package takes the resulting build output and transforms it into such a ClientLib.

A ClientLib will consist of the following files and directories:

- `css/`: CSS files which can be requested in the HTML
- `css.txt` (tells AEM the order and names of files in `css/` so they can be merged)
- `js/`: JavaScript files which can be requested in the HTML
- `js.txt` (tells AEM the order and names of files in `js/` so they can be merged
- `resources/`: Source maps, non-entrypoint code chunks (resulting from code splitting), static assets (e.g. icons), etc.

## Simplified Index Management

If you're building for AEM as a Cloud Service, this project includes a `diff.index` under
`ui.apps/src/main/content/jcr_root/_oak_index` ([documentation](https://oak-indexing.github.io/oakTools/simplified.html)).
Packaging it triggers a benign Maven build warning from the `jackrabbit-nodetypes` validator
(`... is not allowed as child of node with potential default types [nt:folder] ... /oak:index/diff.index`),
because `/oak:index`'s real node type isn't declared in any content package. It's a known false
positive and safe to ignore.

## Maven settings

The project comes with the auto-public repository configured. To setup the repository in your Maven settings, refer to:

    http://helpx.adobe.com/experience-manager/kb/SetUpTheAdobeMavenRepository.html
