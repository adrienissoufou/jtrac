/*
 * Copyright 2002-2005 the original author or authors.
 * 
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
 */

package info.jtrac.wicket;

import info.jtrac.Jtrac;
import info.jtrac.Version;
import info.jtrac.domain.User;
import java.io.Serializable;
import javax.servlet.http.Cookie;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import wicket.Component;
import wicket.markup.html.WebPage;
import wicket.markup.html.basic.Label;
import wicket.markup.html.form.CheckBox;
import wicket.markup.html.form.Form;
import wicket.markup.html.form.PasswordTextField;
import wicket.markup.html.form.TextField;
import wicket.markup.html.link.Link;
import wicket.markup.html.panel.FeedbackPanel;
import wicket.model.AbstractReadOnlyModel;
import wicket.model.BoundCompoundPropertyModel;

/**
 * login page
 */
public class LoginPage extends WebPage {              
    
    protected final Log logger = LogFactory.getLog(getClass());
    
    private Jtrac getJtrac() {
        return ((JtracApplication) getApplication()).getJtrac();
    }    
    
    public LoginPage() {
        // attempt remember-me auto login
        Cookie[] cookies = getWebRequestCycle().getWebRequest().getCookies();
        for(Cookie c : cookies) {
            if(c.getName().equals("jtrac")) {
                String value = c.getValue();
                logger.debug("found jtrac cookie: " + value);                
                if (value != null) {
                    int index = value.indexOf(':');
                    if (index != -1) {
                        String loginName = value.substring(0, index);
                        String encodedPassword = value.substring(index + 1);
                        logger.debug("valid cookie, attempting authentication");
                        User user = (User) getJtrac().loadUserByUsername(loginName);                                              
                        if(encodedPassword.equals(user.getPassword())) {
                            logger.debug("remember me login success, redirecting");
                            ((JtracSession) getSession()).setUser(user);
                            setResponsePage(DashboardPage.class);
                        }
                    }
                }                
            }
        }
        add(new Label("title", getLocalizer().getString("login.title", null)));
        add(new Link("home") {
            public void onClick() {
            }
        });
        add(new LoginForm("form"));
        add(new Label("version", Version.VERSION));
    }
    
    private class LoginForm extends Form {        
        
        public LoginForm(String id) {            
            super(id);          
            add(new FeedbackPanel("feedback"));
            setModel(new BoundCompoundPropertyModel(new LoginFormModel()));
            final TextField loginName = new TextField("loginName");
            loginName.setOutputMarkupId(true);
            add(loginName);
            final PasswordTextField password = new PasswordTextField("password");
            password.setRequired(false);
            password.setOutputMarkupId(true);
            add(password);
            // set focus on right textbox
            getBodyContainer().addOnLoadModifier(new AbstractReadOnlyModel() {
                public Object getObject(Component c) {
                    String markupId;
                    if(loginName.getConvertedInput() == null) {
                        markupId = loginName.getMarkupId();
                    } else {
                        markupId = password.getMarkupId();
                    }
                    return "document.getElementById('" + markupId + "').focus()";
                }
            });            
            add(new CheckBox("rememberMe"));

        }
                
        @Override
        protected void onSubmit() {                    
            LoginFormModel model = (LoginFormModel) getModelObject();
            String loginName = model.getLoginName();
            String password = model.getPassword();
            if(loginName == null || password == null) {
                logger.debug("login failed - login name or password is null");
                error(getLocalizer().getString("login.error", null));
                return;
            }            
            User user = null;
            try {
                user = (User) getJtrac().loadUserByUsername(loginName);
            } catch (UsernameNotFoundException e) {
                logger.debug("login failed - user not found");
                error(getLocalizer().getString("login.error", null));
                return;
            }
            String encodedPassword = getJtrac().encodeClearText(password);
            if (user.getPassword().equals(encodedPassword)) {
                // login successful
                if(model.isRememberMe()) {                    
                    Cookie cookie = new Cookie("jtrac", loginName + ":" + encodedPassword);
                    cookie.setMaxAge(30 * 24 * 60 * 60); // 30 days in seconds 
                    getWebRequestCycle().getWebResponse().addCookie(cookie);
                    logger.debug("remember me requested, cookie added: " + cookie.getValue());
                }                
                ((JtracSession) getSession()).setUser(user);                
                if (!continueToOriginalDestination()) {
                    setResponsePage(DashboardPage.class);
                } 
            } else {
                logger.debug("login failed - password does not match");
                error(getLocalizer().getString("login.error", null));                    
            }                  
        }     
                        
    }     
    
    private class LoginFormModel implements Serializable {
        
        private String loginName;
        private String password;
        private boolean rememberMe;

        public String getLoginName() {
            return loginName;
        }

        public void setLoginName(String loginName) {
            this.loginName = loginName;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }        

        public boolean isRememberMe() {
            return rememberMe;
        }

        public void setRememberMe(boolean rememberMe) {
            this.rememberMe = rememberMe;
        }        
        
    }
        

    
}