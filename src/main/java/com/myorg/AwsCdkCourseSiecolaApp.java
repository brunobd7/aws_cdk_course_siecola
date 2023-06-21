package com.myorg;

import software.amazon.awscdk.App;

public class AwsCdkCourseSiecolaApp {
    public static void main(final String[] args) {
        App app = new App();

        VpcStack vpcStack = new VpcStack(app, "Vpc");

        ClusterStack clusterStack = new ClusterStack(app, "Cluster", vpcStack.getVpc());
        clusterStack.addDependency(vpcStack); /* ADDING DEPENDENCY, BECAUSE OUR CLUSTER REQUIRE A VPC READY TO USE TO BE CREATED INSIDE */

        Service01Stack service01Stack = new Service01Stack(app, "Service01",clusterStack.getCluster());
        service01Stack.addDependency(clusterStack); /* ADDING DEPENDENCY, BECAUSE OUR SERVICE REQUIRE A CLUSTER READY TO USE TO BE CREATED INSIDE */

        app.synth();
    }
}

