#!groovy

def workerNode = "devel10"

pipeline {
	agent {label workerNode}
	triggers {
		githubPush()
	}
	options {
		timestamps()
	}
	tools {
        maven "Maven 3"
    }
	stages {
		stage("clear workspace") {
			steps {
				deleteDir()
				checkout scm
			}
		}
		stage("build") {
			steps {
                script {
                    def status = sh returnStatus: true, script:  """
                        rm -rf \$WORKSPACE/.repo
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo dependency:resolve dependency:resolve-plugins >/dev/null
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo clean
                    """

                    // We want code-coverage and pmd/spotbugs even if unittests fails
                    status += sh returnStatus: true, script:  """
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo pmd:pmd pmd:cpd spotbugs:spotbugs javadoc:aggregate
                    """

                    junit testResults: '**/target/*-reports/TEST-*.xml'

                    def java = scanForIssues tool: [$class: 'Java']
                    def javadoc = scanForIssues tool: [$class: 'JavaDoc']
                    publishIssues issues:[java, javadoc], unstableTotalAll:0

                    def pmd = scanForIssues tool: [$class: 'Pmd'], pattern: '**/target/pmd.xml'
                    publishIssues issues:[pmd], unstableTotalAll:0

                    def cpd = scanForIssues tool: [$class: 'Cpd'], pattern: '**/target/cpd.xml'
                    publishIssues issues:[cpd]

                    def spotbugs = scanForIssues tool: [$class: 'SpotBugs'], pattern: '**/target/spotbugsXml.xml'
                    publishIssues issues:[spotbugs], unstableTotalAll:0

                    if ( status != 0 ) {
                        currentBuild.result = Result.FAILURE
                    }
                }
            }
		}
		stage("deploy") {
			when {
				branch "master"
			}
			steps {
				sh "mvn jar:jar deploy:deploy"
			}
		}
	}
}
