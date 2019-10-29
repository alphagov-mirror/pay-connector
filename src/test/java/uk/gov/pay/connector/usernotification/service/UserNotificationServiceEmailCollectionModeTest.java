package uk.gov.pay.connector.usernotification.service;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import io.dropwizard.setup.Environment;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.pay.connector.app.ConnectorConfiguration;
import uk.gov.pay.connector.app.ExecutorServiceConfig;
import uk.gov.pay.connector.app.NotifyConfiguration;
import uk.gov.pay.connector.gatewayaccount.model.EmailCollectionMode;
import uk.gov.pay.connector.usernotification.govuknotify.NotifyClientFactory;
import uk.gov.service.notify.NotificationClient;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.connector.model.domain.ChargeEntityFixture.aValidChargeEntity;
import static uk.gov.pay.connector.model.domain.ChargeEntityFixture.defaultGatewayAccountEntity;

@RunWith(JUnitParamsRunner.class)
public class UserNotificationServiceEmailCollectionModeTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ConnectorConfiguration connectorConfiguration;
    
    @Mock
    private Environment environment;

    @Mock
    private NotifyClientFactory notifyClientFactory;

    @Mock
    private NotificationClient notificationClient;
    
    @Mock
    private NotifyConfiguration notifyConfiguration;

    @Mock
    private ExecutorServiceConfig mockExecutorConfiguration;

    @Mock
    private MetricRegistry metricRegistry;
    
    @Before
    public void setUp() {
        when(connectorConfiguration.getNotifyConfiguration()).thenReturn(notifyConfiguration);
        when(notifyConfiguration.getEmailTemplateId()).thenReturn("some-template");
        when(notifyConfiguration.getRefundIssuedEmailTemplateId()).thenReturn("another-template");
        when(notifyConfiguration.isEmailNotifyEnabled()).thenReturn(true);

        when(notifyClientFactory.getInstance()).thenReturn(notificationClient);

        when(connectorConfiguration.getExecutorServiceConfig()).thenReturn(mockExecutorConfiguration);
        when(mockExecutorConfiguration.getThreadsPerCpu()).thenReturn(2);

        when(environment.metrics()).thenReturn(metricRegistry);
        when(metricRegistry.histogram(anyString())).thenReturn(mock(Histogram.class));
        when(metricRegistry.counter(anyString())).thenReturn(mock(Counter.class));
    }
    
    @Test
    @Parameters({
            "email@example.com, true",
            "null, false"
    })
    public void determineSendingEmailForOptionalEmailCollectionMode(@Nullable String emailAddress, boolean shouldEmailBeSent) throws Exception {
        var userNotificationService = new UserNotificationService(notifyClientFactory, connectorConfiguration, environment);

        var gatewayAccount = defaultGatewayAccountEntity();
        gatewayAccount.setEmailCollectionMode(EmailCollectionMode.OPTIONAL);
        var chargeEntity = aValidChargeEntity().withEmail(emailAddress).withGatewayAccountEntity(gatewayAccount).build();
        userNotificationService.sendPaymentConfirmedEmail(chargeEntity);

        verify(notificationClient, times(shouldEmailBeSent? 1 : 0)).sendEmail(anyString(), anyString(), anyMap(), isNull());
    }
}
