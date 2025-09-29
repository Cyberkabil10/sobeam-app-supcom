properties([
    parameters([
        string(description: 'branch name to build ex: development or production (please dont use feature/* bugfix/* these are built locally and tested)', name: 'branch'),
        choice(choices: ['development', 'production'], description: "target deployment environment", name: "target"),
        booleanParam(defaultValue: false, description: 'manual override', name: 'manual')
    ])
])
node("master") {

    def TAG

    if (params.manual){
        stage("debug"){
            echo "branchname: ${params.branch}"
            echo "override: ${params.manual}"
        }
        stage("checkout"){
            checkout scmGit(
                branches: [[name: params.branch]],
                userRemoteConfigs: [[credentialsId: 'git-ssh-access',
                url: "https://github.com/ZAKRAOUI-MARWEN/Sobeam.git"]]
            )
        }
        stage("tag") {

			def version = sh(script: "grep -m1 '<version>' pom.xml | sed 's/.*<version>\\(.*\\)<\\/version>.*/\\1/'", returnStdout: true).trim()	

            if (params.branch == "development") {
                TAG = "${version}-${env.BUILD_NUMBER}"
            } else if (params.branch == "main"){
                TAG = "${version}"
            }
        }
    } else {
        stage("debug"){
            // work on automated pull request detect
            echo "GITHUB_PR_STATE: ${GITHUB_PR_STATE}"
            echo "GITHUB_PR_NUMBER: ${GITHUB_PR_NUMBER}"
            echo "GITHUB_PR_SOURCE_BRANCH: ${GITHUB_PR_SOURCE_BRANCH}"
            echo "GITHUB_PR_AUTHOR_EMAIL: ${GITHUB_PR_AUTHOR_EMAIL}"
        }
        stage("checkout"){
            checkout scm
        }
        stage("tag") {
            echo "detect version"
        }
    }

    stage("build") {

        sh "mvn clean install -DskipTests -Ddockerfile.skip=false -Dlicense.skip=true"

        echo "runing build"
    }
    stage("push") {
        echo "pushing project"
    }
    stage("deploy"){
        def buildParameters = [
            [$class: 'StringParameterValue', name: 'tag', value: "${TAG}"], // make this dynamic
            [$class: 'StringParameterValue', name: 'target', value: "${params.target}"] // make this dynamic
        ]
        build job: 'sobeam/cd', parameters: buildParameters, wait : false
    }
}