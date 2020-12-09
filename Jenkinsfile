#!groovy

def workerNode = "devel10"

pipeline {
	agent {label workerNode}
	options {
		timestamps()
	}
	tools {
        maven "Maven 3"
    }
    triggers {
        upstream(upstreamProjects: 'culr/culr-gitops/prod/', threshold: hudson.model.Result.SUCCESS)
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
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo verify pmd:pmd pmd:cpd spotbugs:spotbugs javadoc:aggregate
                    """

                    junit testResults: '**/target/*-reports/TEST-*.xml'

                    def java = scanForIssues tool: [$class: 'Java']
                    def javadoc = scanForIssues tool: [$class: 'JavaDoc']
                    publishIssues issues:[java, javadoc], unstableTotalAll:1

                    def pmd = scanForIssues tool: [$class: 'Pmd'], pattern: '**/target/pmd.xml'
                    publishIssues issues:[pmd], unstableTotalAll:1

                    def spotbugs = scanForIssues tool: [$class: 'SpotBugs'], pattern: '**/target/spotbugsXml.xml'
                    publishIssues issues:[spotbugs], unstableTotalAll:1

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
    post {
        failure {
            script {
                if ("${env.BRANCH_NAME}" == 'master') {
                    slackSend(channel: "meta-notifications",
                            color: 'warning',
                            message: "${env.JOB_NAME} #${env.BUILD_NUMBER} failed and needs attention: ${env.BUILD_URL}",
                            tokenCredentialId: 'slack-global-integration-token')
                }
            }
        }
    }
}
