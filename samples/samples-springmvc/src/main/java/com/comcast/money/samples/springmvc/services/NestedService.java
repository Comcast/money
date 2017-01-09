package com.comcast.money.samples.springmvc.services;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import com.comcast.money.core.Money;
import com.comcast.money.annotations.Traced;
import com.comcast.money.http.client.TraceFriendlyHttpClient;
@Component
public class NestedService {

    private HttpClient httpClient = new TraceFriendlyHttpClient(new DefaultHttpClient());

    @Traced("NESTED_SERVICE")
    public String doSomethingElse(String message) throws Exception {

        double wait = RandomUtil.nextRandom(100, 500);
        Money.Environment().tracer().record("nested-wait", wait);

        // invoking the http client will activate the http tracing...
        callHttpClient();
        return message + ".  Nested service made you wait an additional " + wait + " millseconds.";
    }

    // here, we will test that the http tracing works as expected
    private void callHttpClient() {

        try {
            // it doesn't really matter if it all works for sample purposes
            HttpResponse response = httpClient.execute(new HttpGet("http://www.google.com"));

            // need to call EntityUtils in order to tie into the http trace aspect
            // if you sniff the request, you will also see the X-MoneyTrace request header in the HTTP request
            String result = EntityUtils.toString(response.getEntity());
            Thread.sleep(1);
        } catch(Exception ex) {
            // just eat it
            ex.printStackTrace();
        }
    }
}
