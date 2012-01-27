package controllers;

import play.*;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.modules.paginate.ModelPaginator;
import play.mvc.*;

import java.util.*;

import models.*;

public class Application extends Controller {

    public static void index() {
        render();
    }

    public static void signUp() {
        User user = new User();
        UserProfile user_profile = new UserProfile();
        UserAddress user_address = new UserAddress();
        render(user, user_profile, user_address);
    }

    public static void register(@Valid User user, @Valid UserProfile user_profile, @Valid UserAddress user_address) {
        if (Validation.hasErrors()) {
            flash.error("application.sign.error");
            render("@signUp", user, user_profile, user_address);
        }
        // save user
        user.save();

        // save relationships
        user_profile.user = user;
        user_profile.save();
        user_address.user = user;
        user_address.save();

        flash.success("application.sign.success");
        list();
    }
    
    public static void list() {
//        List<User> users = User.findAll();
//        List<User> users = User.find("select u from User u order by u.id").fetch();
        ModelPaginator users = new ModelPaginator(User.class).orderBy("ID");
        render(users);
    }

    public static void show(Long id) {
        User user = User.findById(id);
        UserProfile user_profile = UserProfile.findByUserId(id);
        UserAddress user_address = UserAddress.findByUserId(id);
        render(user, user_profile, user_address);
    }
}