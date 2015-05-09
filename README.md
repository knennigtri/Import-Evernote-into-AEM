# Evernote Import into AEM Project

Evernote: https://evernote.com/

## Modules

The main parts are:

* core: Java code to actually create the import from Evernote. Currently the integration works specifically with Evernote Clipper
	https://evernote.com/webclipper/
* ui.apps: Code for creating a basic Evernote component and a page to display it on
* ui.content: A basic website to use the Evernote component

## How to build

To build all the modules run in the project root directory the following command with Maven 3:

    mvn clean install

If you have a running AEM instance you can build and package the whole project and deploy into AEM with  

    mvn clean install -PautoInstallPackage
    
Or to deploy it to a publish instance, run

    mvn clean install -PautoInstallPackagePublish
    
Or to deploy only the bundle to the author, run

    mvn clean install -PautoInstallBundle

## Maven settings

The project comes with the auto-public repository configured. To setup the repository in your Maven settings, refer to:

    http://helpx.adobe.com/experience-manager/kb/SetUpTheAdobeMavenRepository.html
