@rem You can set JAVA_HOME to point to your JDK installation.
@rem It's also possible to set it in the IDE: File -> Project Structure -> Gradle settings.

@if "%JAVA_HOME%" == "" (
  @echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
  @echo Please set the JAVA_HOME variable in your environment to match the
  @echo location of your Java installation.
  @exit /B 1
)

@set DIRNAME=%~dp0
@set APP_BASE_NAME=%~n0
@set APP_HOME=%DIRNAME%

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
@set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem Find java.exe
@if exist "%JAVA_HOME%\bin\java.exe" (
  @set JAVA_EXE="%JAVA_HOME%\bin\java.exe"
) else (
  @set JAVA_EXE=java.exe
)

@if not exist "%JAVA_EXE%" (
  @echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
  @echo Please set the JAVA_HOME variable in your environment to match the
  @echo location of your Java installation.
  @exit /B 1
)

@rem --
@rem End of user-editable section.
@rem --

@set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

@set GRADLE_OPTS=-Dorg.gradle.internal.wrapper.GradleWrapperMain=%APP_BASE_NAME%

@echo on
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
