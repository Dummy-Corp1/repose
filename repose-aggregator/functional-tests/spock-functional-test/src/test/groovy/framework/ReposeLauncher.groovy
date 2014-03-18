package framework

import org.apache.http.client.ClientProtocolException
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

abstract class ReposeLauncher {

    abstract void start();

    abstract void stop();

    abstract boolean isUp();

    abstract void enableDebug()

    abstract void addToClassPath(String path)

    def waitForNon500FromUrl(url, int timeoutInSeconds=60, int intervalInSeconds=2) {

        print("Waiting for repose to start at ${url} ")
        waitForCondition(clock, "${timeoutInSeconds}s", "${intervalInSeconds}s") {
            try {
                print(".")
                HttpClient client = new DefaultHttpClient()
                client.execute(new HttpGet(url)).statusLine.statusCode < 500
            } catch (IOException ignored) {
            } catch (ClientProtocolException ignored) {
            }
        }
        println()
    }
}
