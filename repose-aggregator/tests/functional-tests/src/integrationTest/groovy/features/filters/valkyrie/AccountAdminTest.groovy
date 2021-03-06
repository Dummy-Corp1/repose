/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package features.filters.valkyrie

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.openrepose.framework.test.mocks.MockValkyrie
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters
import spock.lang.Unroll

import static org.openrepose.commons.utils.http.OpenStackServiceHeader.ROLES

/**
 * Updated by jennyvo on 11/04/15.
 * if account_admin role make additional call (inventory) to valkyrie to get the full list of devices
 *  Test with:
 *      mock valkyrie return with a list of > 500 devices
 * Update on 01/28/15
 *  - replace client-auth with keystone-v2
 */
@Category(Filters)
class AccountAdminTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static valkyrieEndpoint

    def static MockIdentityV2Service fakeIdentityService
    def static MockValkyrie fakeValkyrie
    def static Map params = [:]

    def static random = new Random()

    def setupSpec() {
        deproxy = new Deproxy()

        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/filters/valkyrie", params);
        repose.configurationProvider.applyConfigs("features/filters/valkyrie/accountadmin", params);

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
        fakeIdentityService.checkTokenValid = true

        fakeValkyrie = new MockValkyrie(properties.valkyriePort)
        valkyrieEndpoint = deproxy.addEndpoint(properties.valkyriePort, 'valkyrie service', null, fakeValkyrie.handler)
    }

    def setup() {
        fakeIdentityService.resetHandlers()
        fakeIdentityService.resetDefaultParameters()
        fakeValkyrie.resetHandlers()
        fakeValkyrie.resetParameters()
    }

    @Unroll
    def "user with account_admin role can access a device it has permissions to for method #method"() {
        given: "a user defined in Identity"
        def tenantId = randomTenant()
        fakeIdentityService.with {
            client_apikey = UUID.randomUUID().toString()
            client_token = UUID.randomUUID().toString()
            client_tenantid = tenantId
        }

        and: "permissions defined in Valkyrie"
        def deviceId = "520707"
        def permission = "account_admin"
        fakeValkyrie.with {
            device_id = deviceId
            device_perm = "edit_product"
            account_perm = permission
            inventory_multiplier = 10
        }

        when: "a #method request is made to access device #deviceID"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/" + deviceId, method: method,
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                ]
        )

        def accountid = fakeValkyrie.tenant_id
        def contactid = fakeIdentityService.contact_id

        then: "the response should be #responseCode and #permission should be in the Requests the X-Roles header"
        mc.receivedResponse.code == "200"
        // user device permission translate to roles
        mc.handlings[0].request.headers.findAll(ROLES).contains(permission)
        mc.handlings[0].request.headers.getFirstValue("x-device-id") == deviceId
        // orphanedhandlings should include the original call + account inventoty call
        mc.orphanedHandlings.request.path.toString().contains("/account/" + accountid + "/permissions/contacts/any/by_contact/" + contactid + "/effective")
        mc.orphanedHandlings.request.path.toString().contains("/account/" + accountid + "/inventory")

        where:
        method << ["HEAD", "GET", "PUT", "POST", "PATCH", "DELETE"]
    }

    @Unroll
    def "Enablebypass false, account_admin user request with an X-Device-Id header value not contained in the user's permissions will not be permitted"() {
        given: "a user defined in Identity"
        fakeIdentityService.with {
            client_apikey = UUID.randomUUID().toString()
            client_token = UUID.randomUUID().toString()
            client_tenantid = randomTenant()
        }

        and: "permissions defined in Valkyrie"
        fakeValkyrie.with {
            account_perm = "account_admin"
        }

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/99999", method: method,
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                ]
        )

        def accountid = fakeValkyrie.tenant_id
        def contactid = fakeIdentityService.contact_id

        then: "the response should be #responseCode and #permission should be in the Requests the X-Roles header"
        mc.receivedResponse.code == "403"
        // the request didn't make it to the origin service
        mc.handlings.isEmpty()
        // orphanedhandlings should include the original call + account inventoty call
        mc.orphanedHandlings.request.path.toString().contains("/account/" + accountid + "/permissions/contacts/any/by_contact/" + contactid + "/effective")
        mc.orphanedHandlings.request.path.toString().contains("/account/" + accountid + "/inventory")

        where:
        method << ["HEAD", "GET", "PUT", "POST", "PATCH", "DELETE"]
    }

    @Unroll
    def "account_admin user request with an X-Device-Id header value that exists in their permissions should be permitted"() {
        given: "a user defined in Identity"
        fakeIdentityService.with {
            client_apikey = UUID.randomUUID().toString()
            client_token = UUID.randomUUID().toString()
            client_tenantid = randomTenant()
        }

        and: "permissions defined in Valkyrie"
        fakeValkyrie.with {
            device_perm = "account_admin"
            device_id = "99999"
        }

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/99999", method: method,
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                ]
        )

        then: "check response"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        where:
        method << ["HEAD", "GET", "PUT", "POST", "PATCH", "DELETE"]
    }

    def String randomTenant() {
        "hybrid:" + random.nextInt()
    }
}
