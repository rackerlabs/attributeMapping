
#############################################################################
# Copyright 2016 Rackspace US, Inc.                                         #
#                                                                           #
# Licensed under the Apache License, Version 2.0 (the "License");           #
# you may not use this file except in compliance with the License.          #
# You may obtain a copy of the License at                                   #
#                                                                           #
#     http://www.apache.org/licenses/LICENSE-2.0                            #
#                                                                           #
# Unless required by applicable law or agreed to in writing, software       #
# distributed under the License is distributed on an "AS IS" BASIS,         #
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  #
# See the License for the specific language governing permissions and       #
# limitations under the License.                                            #
#############################################################################

##
##  Make sure $PROG and $PROG_CLASS is defined
##

if [ ! $PROG ] ; then
    echo 'Missing $PROG environment variable. You should not call runcli.sh directly.'
    exit 1
fi


if [ ! $PROG_CLASS ] ; then
    echo 'Missing $PROG_CLASS environment variable. You should not call runcli.sh directly.'
    exit 1
fi

##
##  Setup nailgun server host, port, and duration
##

if [ ! $MAPPER_NAILGUN_PORT ] ; then
    MAPPER_NAILGUN_PORT=2113
fi


if [ ! $MAPPER_NAILGUN_HOST ] ; then
    MAPPER_NAILGUN_HOST=localhost
fi

if [ ! $MAPPER_NAILGUN_DURATION] ; then
    MAPPER_NAILGUN_DURATION=PT2H
fi

##
## Setup Java options, by default we set ourselves up for JMX, but
## only if we're running nailgun.
##

if [ ! $NO_MAPPER_NAILGUN ] ; then
    MAPPER_NAILGUN_JAVA_OPTS+=" -server -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.local.only=true  "
    MAPPER_NAILGUN_JAVA_OPTS+=" -Dcom.sun.management.jmxremote.authenticate=false "
    MAPPER_NAILGUN_JAVA_OPTS+=" -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=9292 "
    MAPPER_NAILGUN_JAVA_OPTS+=" -Djava.rmi.server.hostname=localhost"
fi

##
##  Discover project directory
##

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"

##
##  Capture CLI arguments
##
CLI_ARGS="$@"

function echoerr() { echo "$@" 1>&2; }

function setupJava() {
    ##
    ##  Locate Java make sure we have the right version
    ##

    if [ ! $JAVA ] ; then
        JAVA=$(which java)
    fi

    command -v $JAVA >/dev/null 2>&1 || { echoerr $PROG ' requires $JAVA enviornment variable or java in the system path!'; exit 1; }

    VERSION=$($JAVA -version 2>&1 | grep ' version' | awk '{ print substr($3, 2, length($3)-2); }')
    JAVA_MINOR=$(echo $VERSION | tr "." " " | cut -d " " -f2)

    if [ "$JAVA_MINOR" -lt "7" ]; then
        echoerr "$PROG requires a Java version of at least 7 to function."
        echoerr "Please install a JRE 1.7 or greater."
        exit 1
    fi
}

function nailgunServer() {
    if [ ! $JAVA_MINOR ] ; then
        setupJava
    fi

    SERVERJAR=$DIR/cli/nailgun-server/target/mapper-nailgun-server*-with-*.jar
    if [ -e $SERVERJAR ]; then
        echoerr "Starting nailgun server..."
        nohup $JAVA $MAPPER_NAILGUN_JAVA_OPTS -jar $SERVERJAR -H $MAPPER_NAILGUN_HOST -p $MAPPER_NAILGUN_PORT -d $MAPPER_NAILGUN_DURATION >/dev/null &
        sleep 1 # Give the server time to start
    fi
    ##
    ## If we can't find the server jar ignore, we can run without nailgun
    ##
}

function nailgunRun() {
    CLIENTEXE=$DIR/cli/nailgun-client/target/ng
    if [ -e $CLIENTEXE ]; then
        $CLIENTEXE $PROG_CLASS --nailgun-port $MAPPER_NAILGUN_PORT --nailgun-server $MAPPER_NAILGUN_HOST $CLI_ARGS
        NGResult=$?
        #
        # Ignore error 227, connection broken. Some clients don't end
        # gracefully.
        #
        if [ $NGResult -eq "227" ]; then
            echo # Output an extra line, since we interrupted mid
                 # stream
            return 0
        fi
        return $NGResult
    else
        return 232 # Cloudn't find nailgun client
    fi
}

function standardRun() {
    if [ ! $JAVA_MINOR ] ; then
        setupJava
    fi

    if [ ! $NO_MAPPER_NAILGUN ] ; then
        echoerr "Nailgun not available...or just starting up...running $PROG directly"
    fi

    WTESTJAR=$DIR/cli/$PROG/target/$PROG*-with-*.jar

    if [ -e $WTESTJAR ]; then
        $JAVA $MAPPER_JAVA_OPTS -jar $WTESTJAR $CLI_ARGS
    else
        echoerr "Couldn't find $WTESTJAR "
        echoerr -n "Perhaps the project has not been built, "
        echoerr "try building the project with mvn install."
    fi
}

if [ $NO_MAPPER_NAILGUN ] ; then
    standardRun
else
    ##
    ##  Check to see if Nailgun is running, if it is, run the command via
    ##  nailgun, if it's not running start the nailgun server
    ##

    (exec 6<>/dev/tcp/$MAPPER_NAILGUN_HOST/$MAPPER_NAILGUN_PORT) &> /dev/null || nailgunServer
    exec 6<&-
    exec 6>&-

    nailgunRun

    ##
    ## Error codes for nailgun range from 226-231, 232 means we can't find ng exec
    ## If we fail with any of these codes, do standard run
    ##
    if [ $? -ge 226 -a $? -le 232 ]
    then
       standardRun
    fi
fi
