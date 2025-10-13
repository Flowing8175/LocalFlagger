#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass any JVM options to Gradle and those will override any settings in this script.
DEFAULT_JVM_OPTS=""

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
msys=false
darwin=false
nonstop=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    msys=true
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
    [ -n "$APP_HOME" ] &&
        APP_HOME=`cygpath --unix "$APP_HOME"`
    [ -n "$JAVA_HOME" ] &&
        JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
fi

# Attempt to find Java
if [ -z "$JAVA_HOME" ] ; then
    if $darwin ; then
        if [ -x '/usr/libexec/java_home' ] ; then
            JAVA_HOME=`/usr/libexec/java_home`
        elif [ -d "/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home" ]; then
            JAVA_HOME="/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home"
        fi
    else
        java_exe_path=$(which java)
        if [ -n "$java_exe_path" ] ; then
            java_exe_path=$(readlink -f "$java_exe_path")
            JAVA_HOME=$(dirname "$(dirname "$java_exe_path")")
        fi
    fi
    if [ -z "$JAVA_HOME" ] ; then
        echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
        echo
        echo "Please set the JAVA_HOME variable in your environment to match the"
        echo "location of your Java installation."
        exit 1
    fi
fi

# Set Java tool options
# Deliberately not quoting JAVA_TOOL_OPTIONS to support multiple options (e.g. -Dprop1=value -Dprop2=value)
if [ -z "$JAVA_TOOL_OPTIONS" ] && [ -n "$JAVA_OPTS" ] && [ -z "$(echo $JAVA_OPTS | grep 'disable-groovy-macro-methods')" ]; then
    export JAVA_TOOL_OPTIONS=$JAVA_OPTS
fi
# Clear JAVA_OPTS to avoid it being used by Gradle
JAVA_OPTS=

# Add -server to the JVM options, if available
if "$JAVA_HOME/bin/java" -server -version 2>&1 | grep -q "Unrecognized option: -server"; then
    # The -server option is not supported, nothing to do
    true
else
    DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS -server"
fi

# Determine the Java version
if [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_VERSION=$("$JAVA_HOME/bin/java" -version 2>&1 | sed -n ';s/.* version "\(.*\)\.\(.*\)\..*".*/\1\2/p;')
    if [ -z "$JAVA_VERSION" ]; then
        # fallback to parsing openjdk version "11.0.2" 2019-01-15
        JAVA_VERSION=$("$JAVA_HOME/bin/java" -version 2>&1 | sed -n ';s/.* version "\(.*\)\.\(.*\)\..*".*/\1/p;' | cut -d. -f1)
    fi
else
    echo "ERROR: Could not determine Java version."
    exit 1
fi

# For Java 9 or later, we need to add the --add-opens options
if [ "$JAVA_VERSION" -ge 9 ]; then
    DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS \
        --add-opens java.base/java.lang=ALL-UNNAMED \
        --add-opens java.base/java.lang.invoke=ALL-UNNAMED \
        --add-opens java.base/java.util=ALL-UNNAMED \
        --add-opens java.prefs/java.util.prefs=ALL-UNNAMED \
        --add-opens java.base/java.nio.charset=ALL-UNNAMED \
        --add-opens java.base/java.net=ALL-UNNAMED \
        --add-opens java.base/java.util.concurrent.atomic=ALL-UNNAMED"
fi

# For Java 16 or later, we need to add the --illegal-access=permit option
if [ "$JAVA_VERSION" -ge 16 ]; then
    DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS --illegal-access=permit"
fi

# For Java 17 or later, we need to add the --add-exports options
if [ "$JAVA_VERSION" -ge 17 ]; then
    DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS \
        --add-exports java.base/sun.nio.ch=ALL-UNNAMED"
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin ; then
    APP_HOME=`cygpath --path --windows "$APP_HOME"`
    JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
fi

# Collect all arguments for the java command
# (Deliberately not quoting JAVA_TOOL_OPTIONS and GRADLE_OPTS to allow multiple options)
exec "$JAVA_HOME/bin/java" ${DEFAULT_JVM_OPTS} ${JAVA_TOOL_OPTIONS} ${GRADLE_OPTS} -Dorg.gradle.appname="$APP_BASE_NAME" -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"